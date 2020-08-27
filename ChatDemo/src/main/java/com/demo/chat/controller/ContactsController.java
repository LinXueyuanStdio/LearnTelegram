package com.demo.chat.controller;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Build;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.SparseArray;

import com.demo.chat.ApplicationLoader;
import com.demo.chat.PhoneFormat.PhoneFormat;
import com.demo.chat.R;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.BuildVars;
import com.demo.chat.messager.FileLog;
import com.demo.chat.messager.NotificationCenter;
import com.demo.chat.messager.Utilities;
import com.demo.chat.model.User;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class ContactsController extends BaseController {
    private String lastContactsVersions = "";
    private int completedRequestsCount;
    public boolean contactsLoaded;
    private boolean contactsSyncInProgress;
    private boolean contactsBookLoaded;

    private boolean ignoreChanges;
    private final Object observerLock = new Object();
    private class MyContentObserver extends ContentObserver {

        private Runnable checkRunnable = () -> {
            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                if (UserConfig.getInstance(a).isClientActivated()) {
                    ContactsController.getInstance(a).checkContacts();
                }
            }
        };

        public MyContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            synchronized (observerLock) {
                if (ignoreChanges) {
                    return;
                }
            }
            Utilities.globalQueue.cancelRunnable(checkRunnable);
            Utilities.globalQueue.postRunnable(checkRunnable, 500);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }
    }

    public static class Contact {
        public int contact_id;
        public String key;
        public String provider;
        public boolean isGoodProvider;
        public ArrayList<String> phones = new ArrayList<>(4);
        public ArrayList<String> phoneTypes = new ArrayList<>(4);
        public ArrayList<String> shortPhones = new ArrayList<>(4);
        public ArrayList<Integer> phoneDeleted = new ArrayList<>(4);
        public String first_name;
        public String last_name;
        public boolean namesFilled;
        public int imported;
        public User user;

        public String getLetter() {
            return getLetter(first_name, last_name);
        }

        public static String getLetter(String first_name, String last_name) {
            String key;
            if (!TextUtils.isEmpty(first_name)) {
                return first_name.substring(0, 1);
            } else if (!TextUtils.isEmpty(last_name)) {
                return last_name.substring(0, 1);
            } else {
                return "#";
            }
        }
    }

    private String[] projectionPhones = {
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.RawContacts.ACCOUNT_TYPE,
            };
    private String[] projectionNames = {
            ContactsContract.CommonDataKinds.StructuredName.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
            ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
            };

    public HashMap<String, Contact> contactsBook = new HashMap<>();
    public HashMap<String, Contact> contactsBookSPhones = new HashMap<>();
    public ArrayList<Contact> phoneBookContacts = new ArrayList<>();
    public HashMap<String, ArrayList<Object>> phoneBookSectionsDict = new HashMap<>();
    public ArrayList<String> phoneBookSectionsArray = new ArrayList<>();

    private HashMap<String, String> sectionsToReplace = new HashMap<>();

    private static volatile ContactsController[] Instance = new ContactsController[UserConfig.MAX_ACCOUNT_COUNT];
    public static ContactsController getInstance(int num) {
        ContactsController localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (ContactsController.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new ContactsController(num);
                }
            }
        }
        return localInstance;
    }

    public ContactsController(int instance) {
        super(instance);
        SharedPreferences preferences = MessagesController.getMainSettings(currentAccount);
        if (preferences.getBoolean("needGetStatuses", false)) {
            reloadContactsStatuses();
        }

        sectionsToReplace.put("À", "A");
        sectionsToReplace.put("Á", "A");
        sectionsToReplace.put("Ä", "A");
        sectionsToReplace.put("Ù", "U");
        sectionsToReplace.put("Ú", "U");
        sectionsToReplace.put("Ü", "U");
        sectionsToReplace.put("Ì", "I");
        sectionsToReplace.put("Í", "I");
        sectionsToReplace.put("Ï", "I");
        sectionsToReplace.put("È", "E");
        sectionsToReplace.put("É", "E");
        sectionsToReplace.put("Ê", "E");
        sectionsToReplace.put("Ë", "E");
        sectionsToReplace.put("Ò", "O");
        sectionsToReplace.put("Ó", "O");
        sectionsToReplace.put("Ö", "O");
        sectionsToReplace.put("Ç", "C");
        sectionsToReplace.put("Ñ", "N");
        sectionsToReplace.put("Ÿ", "Y");
        sectionsToReplace.put("Ý", "Y");
        sectionsToReplace.put("Ţ", "Y");

        if (instance == 0) {
            Utilities.globalQueue.postRunnable(() -> {
                try {
                    if (hasContactsPermission()) {
                        ApplicationLoader.applicationContext.getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, new MyContentObserver());
                    }
                } catch (Throwable ignore) {

                }
            });
        }
    }


    private boolean hasContactsPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            return ApplicationLoader.applicationContext.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        }
        Cursor cursor = null;
        try {
            ContentResolver cr = ApplicationLoader.applicationContext.getContentResolver();
            cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projectionPhones, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                return false;
            }
        } catch (Throwable e) {
            FileLog.e(e);
        } finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        return true;
    }


    public void addContact(User user, boolean exception) {
        if (user == null) {
            return;
        }
    }

    public void deleteContact(final ArrayList<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
    }

    private void reloadContactsStatuses() {
        saveContactsLoadTime();
        SharedPreferences preferences = MessagesController.getMainSettings(currentAccount);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("needGetStatuses", true).commit();
    }


    private void saveContactsLoadTime() {
        try {
            SharedPreferences preferences = MessagesController.getMainSettings(currentAccount);
            preferences.edit().putLong("lastReloadStatusTime", System.currentTimeMillis()).commit();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void forceImportContacts() {
        Utilities.globalQueue.postRunnable(() -> {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("force import contacts");
            }
            performSyncPhoneBook(new HashMap<>(), true, true, true, true, false, false);
        });
    }

    protected void performSyncPhoneBook(final HashMap<String, Contact> contactHashMap, final boolean request, final boolean first, final boolean schedule, final boolean force, final boolean checkCount, final boolean canceled) {
        if (!first && !contactsBookLoaded) {
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            int newPhonebookContacts = 0;
            int serverContactsInPhonebook = 0;
            boolean disableDeletion = true;
            //disable contacts deletion, because phone numbers can't be compared due to different numbers format
            /*if (schedule) {
                try {
                    AccountManager am = AccountManager.get(ApplicationLoader.applicationContext);
                    Account[] accounts = am.getAccountsByType("org.telegram.account");
                    boolean recreateAccount = false;
                    if (getUserConfig().isClientActivated()) {
                        if (accounts.length != 1) {
                            FileLog.e("detected account deletion!");
                            currentAccount = new Account(getUserConfig().getCurrentUser().phone, "org.telegram.account");
                            am.addAccountExplicitly(currentAccount, "", null);
                            AndroidUtilities.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    performWriteContactsToPhoneBook();
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }*/

            HashMap<String, Contact> contactShortHashMap = new HashMap<>();
            for (HashMap.Entry<String, Contact> entry : contactHashMap.entrySet()) {
                Contact c = entry.getValue();
                for (int a = 0; a < c.shortPhones.size(); a++) {
                    contactShortHashMap.put(c.shortPhones.get(a), c);
                }
            }

            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("start read contacts from phone");
            }
            if (!schedule) {
                checkContactsInternal();
            }
            final HashMap<String, Contact> contactsMap = readContactsFromPhoneBook();
            final HashMap<String, ArrayList<Object>> phoneBookSectionsDictFinal = new HashMap<>();
            final HashMap<String, Contact> phoneBookByShortPhonesFinal = new HashMap<>();
            final ArrayList<String> phoneBookSectionsArrayFinal = new ArrayList<>();

            for (HashMap.Entry<String, Contact> entry : contactsMap.entrySet()) {
                Contact contact = entry.getValue();
                for (int a = 0, size = contact.shortPhones.size(); a < size; a++) {
                    String phone = contact.shortPhones.get(a);
                    phoneBookByShortPhonesFinal.put(phone.substring(Math.max(0, phone.length() - 7)), contact);
                }

                String key = contact.getLetter();
                ArrayList<Object> arrayList = phoneBookSectionsDictFinal.get(key);
                if (arrayList == null) {
                    arrayList = new ArrayList<>();
                    phoneBookSectionsDictFinal.put(key, arrayList);
                    phoneBookSectionsArrayFinal.add(key);
                }
                arrayList.add(contact);
            }

            final HashMap<String, Contact> contactsBookShort = new HashMap<>();
            int alreadyImportedContacts = contactHashMap.size();

            ArrayList<TLRPC.TL_inputPhoneContact> toImport = new ArrayList<>();
            if (!contactHashMap.isEmpty()) {
                for (HashMap.Entry<String, Contact> pair : contactsMap.entrySet()) {
                    String id = pair.getKey();
                    Contact value = pair.getValue();
                    Contact existing = contactHashMap.get(id);
                    if (existing == null) {
                        for (int a = 0; a < value.shortPhones.size(); a++) {
                            Contact c = contactShortHashMap.get(value.shortPhones.get(a));
                            if (c != null) {
                                existing = c;
                                id = existing.key;
                                break;
                            }
                        }
                    }
                    if (existing != null) {
                        value.imported = existing.imported;
                    }

                    boolean nameChanged = existing != null && (!TextUtils.isEmpty(value.first_name) && !existing.first_name.equals(value.first_name) || !TextUtils.isEmpty(value.last_name) && !existing.last_name.equals(value.last_name));
                    if (existing == null || nameChanged) {
                        for (int a = 0; a < value.phones.size(); a++) {
                            String sphone = value.shortPhones.get(a);
                            String sphone9 = sphone.substring(Math.max(0, sphone.length() - 7));
                            contactsBookShort.put(sphone, value);
                            if (existing != null) {
                                int index = existing.shortPhones.indexOf(sphone);
                                if (index != -1) {
                                    Integer deleted = existing.phoneDeleted.get(index);
                                    value.phoneDeleted.set(a, deleted);
                                    if (deleted == 1) {
                                        continue;
                                    }
                                }
                            }
                            if (request) {
                                if (!nameChanged) {
                                    if (contactsByPhone.containsKey(sphone)) {
                                        serverContactsInPhonebook++;
                                        continue;
                                    }
                                    newPhonebookContacts++;
                                }

                                TLRPC.TL_inputPhoneContact imp = new TLRPC.TL_inputPhoneContact();
                                imp.client_id = value.contact_id;
                                imp.client_id |= ((long) a) << 32;
                                imp.first_name = value.first_name;
                                imp.last_name = value.last_name;
                                imp.phone = value.phones.get(a);
                                toImport.add(imp);
                            }
                        }
                        if (existing != null) {
                            contactHashMap.remove(id);
                        }
                    } else {
                        for (int a = 0; a < value.phones.size(); a++) {
                            String sphone = value.shortPhones.get(a);
                            String sphone9 = sphone.substring(Math.max(0, sphone.length() - 7));
                            contactsBookShort.put(sphone, value);
                            int index = existing.shortPhones.indexOf(sphone);
                            boolean emptyNameReimport = false;
                            if (request) {
                                TLRPC.TL_contact contact = contactsByPhone.get(sphone);
                                if (contact != null) {
                                    TLRPC.User user = getMessagesController().getUser(contact.user_id);
                                    if (user != null) {
                                        serverContactsInPhonebook++;
                                        if (TextUtils.isEmpty(user.first_name) && TextUtils.isEmpty(user.last_name) && (!TextUtils.isEmpty(value.first_name) || !TextUtils.isEmpty(value.last_name))) {
                                            index = -1;
                                            emptyNameReimport = true;
                                        }
                                    }
                                } else if (contactsByShortPhone.containsKey(sphone9)) {
                                    serverContactsInPhonebook++;
                                }
                            }
                            if (index == -1) {
                                if (request) {
                                    if (!emptyNameReimport) {
                                        TLRPC.TL_contact contact = contactsByPhone.get(sphone);
                                        if (contact != null) {
                                            TLRPC.User user = getMessagesController().getUser(contact.user_id);
                                            if (user != null) {
                                                serverContactsInPhonebook++;
                                                String firstName = user.first_name != null ? user.first_name : "";
                                                String lastName = user.last_name != null ? user.last_name : "";
                                                if (firstName.equals(value.first_name) && lastName.equals(value.last_name) || TextUtils.isEmpty(value.first_name) && TextUtils.isEmpty(value.last_name)) {
                                                    continue;
                                                }
                                            } else {
                                                newPhonebookContacts++;
                                            }
                                        } else if (contactsByShortPhone.containsKey(sphone9)) {
                                            serverContactsInPhonebook++;
                                        }
                                    }

                                    TLRPC.TL_inputPhoneContact imp = new TLRPC.TL_inputPhoneContact();
                                    imp.client_id = value.contact_id;
                                    imp.client_id |= ((long) a) << 32;
                                    imp.first_name = value.first_name;
                                    imp.last_name = value.last_name;
                                    imp.phone = value.phones.get(a);
                                    toImport.add(imp);
                                }
                            } else {
                                value.phoneDeleted.set(a, existing.phoneDeleted.get(index));
                                existing.phones.remove(index);
                                existing.shortPhones.remove(index);
                                existing.phoneDeleted.remove(index);
                                existing.phoneTypes.remove(index);
                            }
                        }
                        if (existing.phones.isEmpty()) {
                            contactHashMap.remove(id);
                        }
                    }
                }
                if (!first && contactHashMap.isEmpty() && toImport.isEmpty() && alreadyImportedContacts == contactsMap.size()) {
                    if (BuildVars.LOGS_ENABLED) {
                        FileLog.d("contacts not changed!");
                    }
                    return;
                }
                if (request && !contactHashMap.isEmpty() && !contactsMap.isEmpty()) {
                    if (toImport.isEmpty()) {
                        getMessagesStorage().putCachedPhoneBook(contactsMap, false, false);
                    }
                    if (!disableDeletion && !contactHashMap.isEmpty()) {
                        AndroidUtilities.runOnUIThread(() -> {
                            /*if (BuildVars.DEBUG_VERSION) {
                                FileLog.e("need delete contacts");
                                for (HashMap.Entry<Integer, Contact> c : contactHashMap.entrySet()) {
                                    Contact contact = c.getValue();
                                    FileLog.e("delete contact " + contact.first_name + " " + contact.last_name);
                                    for (String phone : contact.phones) {
                                        FileLog.e(phone);
                                    }
                                }
                            }*/

                            final ArrayList<TLRPC.User> toDelete = new ArrayList<>();
                            if (contactHashMap != null && !contactHashMap.isEmpty()) {
                                try {
                                    final HashMap<String, TLRPC.User> contactsPhonesShort = new HashMap<>();

                                    for (int a = 0; a < contacts.size(); a++) {
                                        TLRPC.TL_contact value = contacts.get(a);
                                        TLRPC.User user = getMessagesController().getUser(value.user_id);
                                        if (user == null || TextUtils.isEmpty(user.phone)) {
                                            continue;
                                        }
                                        contactsPhonesShort.put(user.phone, user);
                                    }
                                    int removed = 0;
                                    for (HashMap.Entry<String, Contact> entry : contactHashMap.entrySet()) {
                                        Contact contact = entry.getValue();
                                        boolean was = false;
                                        for (int a = 0; a < contact.shortPhones.size(); a++) {
                                            String phone = contact.shortPhones.get(a);
                                            TLRPC.User user = contactsPhonesShort.get(phone);
                                            if (user != null) {
                                                was = true;
                                                toDelete.add(user);
                                                contact.shortPhones.remove(a);
                                                a--;
                                            }
                                        }
                                        if (!was || contact.shortPhones.size() == 0) {
                                            removed++;
                                        }
                                    }
                                } catch (Exception e) {
                                    FileLog.e(e);
                                }
                            }

                            if (!toDelete.isEmpty()) {
                                deleteContact(toDelete);
                            }
                        });
                    }
                }
            } else if (request) {
                for (HashMap.Entry<String, Contact> pair : contactsMap.entrySet()) {
                    Contact value = pair.getValue();
                    String key = pair.getKey();
                    for (int a = 0; a < value.phones.size(); a++) {
                        if (!force) {
                            String sphone = value.shortPhones.get(a);
                            String sphone9 = sphone.substring(Math.max(0, sphone.length() - 7));
                            TLRPC.TL_contact contact = contactsByPhone.get(sphone);
                            if (contact != null) {
                                TLRPC.User user = getMessagesController().getUser(contact.user_id);
                                if (user != null) {
                                    serverContactsInPhonebook++;
                                    String firstName = user.first_name != null ? user.first_name : "";
                                    String lastName = user.last_name != null ? user.last_name : "";
                                    if (firstName.equals(value.first_name) && lastName.equals(value.last_name) || TextUtils.isEmpty(value.first_name) && TextUtils.isEmpty(value.last_name)) {
                                        continue;
                                    }
                                }
                            } else if (contactsByShortPhone.containsKey(sphone9)) {
                                serverContactsInPhonebook++;
                            }
                        }
                        TLRPC.TL_inputPhoneContact imp = new TLRPC.TL_inputPhoneContact();
                        imp.client_id = value.contact_id;
                        imp.client_id |= ((long) a) << 32;
                        imp.first_name = value.first_name;
                        imp.last_name = value.last_name;
                        imp.phone = value.phones.get(a);
                        toImport.add(imp);
                    }
                }
            }

            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("done processing contacts");
            }

            if (request) {
                if (!toImport.isEmpty()) {
                    if (BuildVars.LOGS_ENABLED) {
                        FileLog.e("start import contacts");
                        /*for (TLRPC.TL_inputPhoneContact contact : toImport) {
                            FileLog.e("add contact " + contact.first_name + " " + contact.last_name + " " + contact.phone);
                        }*/
                    }

                    final int checkType;
                    if (checkCount && newPhonebookContacts != 0) {
                        if (newPhonebookContacts >= 30) {
                            checkType = 1;
                        } else if (first && alreadyImportedContacts == 0 && contactsByPhone.size() - serverContactsInPhonebook > contactsByPhone.size() / 3 * 2) {
                            checkType = 2;
                        } else {
                            checkType = 0;
                        }
                    } else {
                        checkType = 0;
                    }
                    if (BuildVars.LOGS_ENABLED) {
                        FileLog.d("new phone book contacts " + newPhonebookContacts + " serverContactsInPhonebook " + serverContactsInPhonebook + " totalContacts " + contactsByPhone.size());
                    }
                    if (checkType != 0) {
                        AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.hasNewContactsToImport, checkType, contactHashMap, first, schedule));
                        return;
                    } else if (canceled) {
                        Utilities.stageQueue.postRunnable(() -> {
                            contactsBookSPhones = contactsBookShort;
                            contactsBook = contactsMap;
                            contactsSyncInProgress = false;
                            contactsBookLoaded = true;
                            if (first) {
                                contactsLoaded = true;
                            }
                            if (!delayedContactsUpdate.isEmpty() && contactsLoaded) {
                                applyContactsUpdates(delayedContactsUpdate, null, null, null);
                                delayedContactsUpdate.clear();
                            }
                            getMessagesStorage().putCachedPhoneBook(contactsMap, false, false);
                            AndroidUtilities.runOnUIThread(() -> {
                                mergePhonebookAndTelegramContacts(phoneBookSectionsDictFinal, phoneBookSectionsArrayFinal, phoneBookByShortPhonesFinal);
                                updateUnregisteredContacts();
                                getNotificationCenter().postNotificationName(NotificationCenter.contactsDidLoad);
                                getNotificationCenter().postNotificationName(NotificationCenter.contactsImported);
                            });
                        });
                        return;
                    }

                    final boolean[] hasErrors = new boolean[]{false};
                    final HashMap<String, Contact> contactsMapToSave = new HashMap<>(contactsMap);
                    final SparseArray<String> contactIdToKey = new SparseArray<>();
                    for (HashMap.Entry<String, Contact> entry : contactsMapToSave.entrySet()) {
                        Contact value = entry.getValue();
                        contactIdToKey.put(value.contact_id, value.key);
                    }
                    completedRequestsCount = 0;
                    final int count = (int) Math.ceil(toImport.size() / 500.0);
                    for (int a = 0; a < count; a++) {
                        final TLRPC.TL_contacts_importContacts req = new TLRPC.TL_contacts_importContacts();
                        int start = a * 500;
                        int end = Math.min(start + 500, toImport.size());
                        req.contacts = new ArrayList<>(toImport.subList(start, end));
                        getConnectionsManager().sendRequest(req, (response, error) -> {
                            completedRequestsCount++;
                            if (error == null) {
                                if (BuildVars.LOGS_ENABLED) {
                                    FileLog.d("contacts imported");
                                }
                                final TLRPC.TL_contacts_importedContacts res = (TLRPC.TL_contacts_importedContacts) response;
                                if (!res.retry_contacts.isEmpty()) {
                                    for (int a1 = 0; a1 < res.retry_contacts.size(); a1++) {
                                        long id = res.retry_contacts.get(a1);
                                        contactsMapToSave.remove(contactIdToKey.get((int) id));
                                    }
                                    hasErrors[0] = true;
                                    if (BuildVars.LOGS_ENABLED) {
                                        FileLog.d("result has retry contacts");
                                    }
                                }
                                for (int a1 = 0; a1 < res.popular_invites.size(); a1++) {
                                    TLRPC.TL_popularContact popularContact = res.popular_invites.get(a1);
                                    Contact contact = contactsMap.get(contactIdToKey.get((int) popularContact.client_id));
                                    if (contact != null) {
                                        contact.imported = popularContact.importers;
                                    }
                                }

                                /*if (BuildVars.LOGS_ENABLED) {
                                    for (TLRPC.User user : res.users) {
                                        FileLog.e("received user " + user.first_name + " " + user.last_name + " " + user.phone);
                                    }
                                }*/
                                getMessagesStorage().putUsersAndChats(res.users, null, true, true);
                                ArrayList<TLRPC.TL_contact> cArr = new ArrayList<>();
                                for (int a1 = 0; a1 < res.imported.size(); a1++) {
                                    TLRPC.TL_contact contact = new TLRPC.TL_contact();
                                    contact.user_id = res.imported.get(a1).user_id;
                                    cArr.add(contact);
                                }
                                processLoadedContacts(cArr, res.users, 2);
                            } else {
                                for (int a1 = 0; a1 < req.contacts.size(); a1++) {
                                    TLRPC.TL_inputPhoneContact contact = req.contacts.get(a1);
                                    contactsMapToSave.remove(contactIdToKey.get((int) contact.client_id));
                                }
                                hasErrors[0] = true;
                                if (BuildVars.LOGS_ENABLED) {
                                    FileLog.d("import contacts error " + error.text);
                                }
                            }
                            if (completedRequestsCount == count) {
                                if (!contactsMapToSave.isEmpty()) {
                                    getMessagesStorage().putCachedPhoneBook(contactsMapToSave, false, false);
                                }
                                Utilities.stageQueue.postRunnable(() -> {
                                    contactsBookSPhones = contactsBookShort;
                                    contactsBook = contactsMap;
                                    contactsSyncInProgress = false;
                                    contactsBookLoaded = true;
                                    if (first) {
                                        contactsLoaded = true;
                                    }
                                    if (!delayedContactsUpdate.isEmpty() && contactsLoaded) {
                                        applyContactsUpdates(delayedContactsUpdate, null, null, null);
                                        delayedContactsUpdate.clear();
                                    }
                                    AndroidUtilities.runOnUIThread(() -> {
                                        mergePhonebookAndTelegramContacts(phoneBookSectionsDictFinal, phoneBookSectionsArrayFinal, phoneBookByShortPhonesFinal);
                                        getNotificationCenter().postNotificationName(NotificationCenter.contactsImported);
                                    });
                                    if (hasErrors[0]) {
                                        Utilities.globalQueue.postRunnable(() -> getMessagesStorage().getCachedPhoneBook(true), 60000 * 5);
                                    }
                                });
                            }
                        }, ConnectionsManager.RequestFlagFailOnServerErrors | ConnectionsManager.RequestFlagCanCompress);
                    }
                } else {
                    Utilities.stageQueue.postRunnable(() -> {
                        contactsBookSPhones = contactsBookShort;
                        contactsBook = contactsMap;
                        contactsSyncInProgress = false;
                        contactsBookLoaded = true;
                        if (first) {
                            contactsLoaded = true;
                        }
                        if (!delayedContactsUpdate.isEmpty() && contactsLoaded) {
                            applyContactsUpdates(delayedContactsUpdate, null, null, null);
                            delayedContactsUpdate.clear();
                        }
                        AndroidUtilities.runOnUIThread(() -> {
                            mergePhonebookAndTelegramContacts(phoneBookSectionsDictFinal, phoneBookSectionsArrayFinal, phoneBookByShortPhonesFinal);
                            updateUnregisteredContacts();
                            getNotificationCenter().postNotificationName(NotificationCenter.contactsDidLoad);
                            getNotificationCenter().postNotificationName(NotificationCenter.contactsImported);
                        });
                    });
                }
            } else {
                Utilities.stageQueue.postRunnable(() -> {
                    contactsBookSPhones = contactsBookShort;
                    contactsBook = contactsMap;
                    contactsSyncInProgress = false;
                    contactsBookLoaded = true;
                    if (first) {
                        contactsLoaded = true;
                    }
                    if (!delayedContactsUpdate.isEmpty() && contactsLoaded && contactsBookLoaded) {
                        applyContactsUpdates(delayedContactsUpdate, null, null, null);
                        delayedContactsUpdate.clear();
                    }
                    AndroidUtilities.runOnUIThread(() -> mergePhonebookAndTelegramContacts(phoneBookSectionsDictFinal, phoneBookSectionsArrayFinal, phoneBookByShortPhonesFinal));
                });
                if (!contactsMap.isEmpty()) {
                    getMessagesStorage().putCachedPhoneBook(contactsMap, false, false);
                }
            }
        });
    }

    private HashMap<String, Contact> readContactsFromPhoneBook() {
        if (!getUserConfig().syncContacts) {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("contacts sync disabled");
            }
            return new HashMap<>();
        }
        if (!hasContactsPermission()) {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("app has no contacts permissions");
            }
            return new HashMap<>();
        }
        Cursor pCur = null;
        HashMap<String, Contact> contactsMap = null;
        try {
            StringBuilder escaper = new StringBuilder();

            ContentResolver cr = ApplicationLoader.applicationContext.getContentResolver();

            HashMap<String, Contact> shortContacts = new HashMap<>();
            ArrayList<String> idsArr = new ArrayList<>();
            pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projectionPhones, null, null, null);

            int lastContactId = 1;
            if (pCur != null) {
                int count = pCur.getCount();
                if (count > 0) {
                    if (contactsMap == null) {
                        contactsMap = new HashMap<>(count);
                    }
                    while (pCur.moveToNext()) {
                        String number = pCur.getString(1);
                        String accountType = pCur.getString(5);
                        if (accountType == null) {
                            accountType = "";
                        }
                        boolean isGoodAccountType = accountType.indexOf(".sim") != 0;
                        if (TextUtils.isEmpty(number)) {
                            continue;
                        }
                        number = PhoneFormat.stripExceptNumbers(number, true);
                        if (TextUtils.isEmpty(number)) {
                            continue;
                        }

                        String shortNumber = number;

                        if (number.startsWith("+")) {
                            shortNumber = number.substring(1);
                        }

                        String lookup_key = pCur.getString(0);
                        escaper.setLength(0);
                        DatabaseUtils.appendEscapedSQLString(escaper, lookup_key);
                        String key = escaper.toString();

                        Contact existingContact = shortContacts.get(shortNumber);
                        if (existingContact != null) {
                            if (!existingContact.isGoodProvider && !accountType.equals(existingContact.provider)) {
                                escaper.setLength(0);
                                DatabaseUtils.appendEscapedSQLString(escaper, existingContact.key);
                                idsArr.remove(escaper.toString());
                                idsArr.add(key);
                                existingContact.key = lookup_key;
                                existingContact.isGoodProvider = isGoodAccountType;
                                existingContact.provider = accountType;
                            }
                            continue;
                        }

                        if (!idsArr.contains(key)) {
                            idsArr.add(key);
                        }

                        int type = pCur.getInt(2);
                        Contact contact = contactsMap.get(lookup_key);
                        if (contact == null) {
                            contact = new Contact();
                            String displayName = pCur.getString(4);
                            if (displayName == null) {
                                displayName = "";
                            } else {
                                displayName = displayName.trim();
                            }
                            if (isNotValidNameString(displayName)) {
                                contact.first_name = displayName;
                                contact.last_name = "";
                            } else {
                                int spaceIndex = displayName.lastIndexOf(' ');
                                if (spaceIndex != -1) {
                                    contact.first_name = displayName.substring(0, spaceIndex).trim();
                                    contact.last_name = displayName.substring(spaceIndex + 1).trim();
                                } else {
                                    contact.first_name = displayName;
                                    contact.last_name = "";
                                }
                            }
                            contact.provider = accountType;
                            contact.isGoodProvider = isGoodAccountType;
                            contact.key = lookup_key;
                            contact.contact_id = lastContactId++;
                            contactsMap.put(lookup_key, contact);
                        }

                        contact.shortPhones.add(shortNumber);
                        contact.phones.add(number);
                        contact.phoneDeleted.add(0);

                        if (type == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
                            String custom = pCur.getString(3);
                            contact.phoneTypes.add(custom != null ? custom : LocaleController.getString("PhoneMobile", R.string.PhoneMobile));
                        } else if (type == ContactsContract.CommonDataKinds.Phone.TYPE_HOME) {
                            contact.phoneTypes.add(LocaleController.getString("PhoneHome", R.string.PhoneHome));
                        } else if (type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
                            contact.phoneTypes.add(LocaleController.getString("PhoneMobile", R.string.PhoneMobile));
                        } else if (type == ContactsContract.CommonDataKinds.Phone.TYPE_WORK) {
                            contact.phoneTypes.add(LocaleController.getString("PhoneWork", R.string.PhoneWork));
                        } else if (type == ContactsContract.CommonDataKinds.Phone.TYPE_MAIN) {
                            contact.phoneTypes.add(LocaleController.getString("PhoneMain", R.string.PhoneMain));
                        } else {
                            contact.phoneTypes.add(LocaleController.getString("PhoneOther", R.string.PhoneOther));
                        }
                        shortContacts.put(shortNumber, contact);
                    }
                }
                try {
                    pCur.close();
                } catch (Exception ignore) {

                }
                pCur = null;
            }
            String ids = TextUtils.join(",", idsArr);

            pCur = cr.query(ContactsContract.Data.CONTENT_URI, projectionNames, ContactsContract.CommonDataKinds.StructuredName.LOOKUP_KEY + " IN (" + ids + ") AND " + ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE + "'", null, null);
            if (pCur != null) {
                while (pCur.moveToNext()) {
                    String lookup_key = pCur.getString(0);
                    String fname = pCur.getString(1);
                    String sname = pCur.getString(2);
                    String mname = pCur.getString(3);
                    Contact contact = contactsMap.get(lookup_key);
                    if (contact != null && !contact.namesFilled) {
                        if (contact.isGoodProvider) {
                            if (fname != null) {
                                contact.first_name = fname;
                            } else {
                                contact.first_name = "";
                            }
                            if (sname != null) {
                                contact.last_name = sname;
                            } else {
                                contact.last_name = "";
                            }
                            if (!TextUtils.isEmpty(mname)) {
                                if (!TextUtils.isEmpty(contact.first_name)) {
                                    contact.first_name += " " + mname;
                                } else {
                                    contact.first_name = mname;
                                }
                            }
                        } else {
                            if (!isNotValidNameString(fname) && (contact.first_name.contains(fname) || fname.contains(contact.first_name)) ||
                                    !isNotValidNameString(sname) && (contact.last_name.contains(sname) || fname.contains(contact.last_name))) {
                                if (fname != null) {
                                    contact.first_name = fname;
                                } else {
                                    contact.first_name = "";
                                }
                                if (!TextUtils.isEmpty(mname)) {
                                    if (!TextUtils.isEmpty(contact.first_name)) {
                                        contact.first_name += " " + mname;
                                    } else {
                                        contact.first_name = mname;
                                    }
                                }
                                if (sname != null) {
                                    contact.last_name = sname;
                                } else {
                                    contact.last_name = "";
                                }
                            }
                        }
                        contact.namesFilled = true;
                    }
                }
                try {
                    pCur.close();
                } catch (Exception ignore) {

                }
                pCur = null;
            }
        } catch (Throwable e) {
            FileLog.e(e);
            if (contactsMap != null) {
                contactsMap.clear();
            }
        } finally {
            try {
                if (pCur != null) {
                    pCur.close();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        /*if (BuildVars.LOGS_ENABLED && contactsMap != null) {
            for (HashMap.Entry<String, Contact> entry : contactsMap.entrySet()) {
                Contact contact = entry.getValue();
                FileLog.e("contact = " + contact.first_name + " " + contact.last_name);
                if (contact.first_name.length() == 0 && contact.last_name.length() == 0 && contact.phones.size() > 0) {
                    FileLog.e("warning, empty name for contact = " + contact.key);
                }
                FileLog.e("phones:");
                for (String s : contact.phones) {
                    FileLog.e("phone = " + s);
                }
                FileLog.e("short phones:");
                for (String s : contact.shortPhones) {
                    FileLog.e("short phone = " + s);
                }
            }
        }*/
        return contactsMap != null ? contactsMap : new HashMap<>();
    }
    private boolean isNotValidNameString(String src) {
        if (TextUtils.isEmpty(src)) {
            return true;
        }
        int count = 0;
        for (int a = 0, len = src.length(); a < len; a++) {
            char c = src.charAt(a);
            if (c >= '0' && c <= '9') {
                count++;
            }
        }
        return count > 3;
    }

    private boolean checkContactsInternal() {
        boolean reload = false;
        try {
            if (!hasContactsPermission()) {
                return false;
            }
            ContentResolver cr = ApplicationLoader.applicationContext.getContentResolver();
            try (Cursor pCur = cr.query(ContactsContract.RawContacts.CONTENT_URI, new String[]{ContactsContract.RawContacts.VERSION}, null, null, null)) {
                if (pCur != null) {
                    StringBuilder currentVersion = new StringBuilder();
                    while (pCur.moveToNext()) {
                        currentVersion.append(pCur.getString(pCur.getColumnIndex(ContactsContract.RawContacts.VERSION)));
                    }
                    String newContactsVersion = currentVersion.toString();
                    if (lastContactsVersions.length() != 0 && !lastContactsVersions.equals(newContactsVersion)) {
                        reload = true;
                    }
                    lastContactsVersions = newContactsVersion;
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return reload;
    }

    private Account systemAccount;
    public void checkAppAccount() {
        AccountManager am = AccountManager.get(ApplicationLoader.applicationContext);
        try {
            Account[] accounts = am.getAccountsByType("org.telegram.messenger");
            systemAccount = null;
            for (int a = 0; a < accounts.length; a++) {
                Account acc = accounts[a];
                boolean found = false;
                for (int b = 0; b < UserConfig.MAX_ACCOUNT_COUNT; b++) {
                    User user = UserConfig.getInstance(b).getCurrentUser();
                    if (user != null) {
                        if (acc.name.equals("" + user.id)) {
                            if (b == currentAccount) {
                                systemAccount = acc;
                            }
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    try {
                        am.removeAccount(accounts[a], null, null);
                    } catch (Exception ignore) {

                    }
                }

            }
        } catch (Throwable ignore) {

        }
        if (getUserConfig().isClientActivated()) {
            readContacts();
            if (systemAccount == null) {
                try {
                    systemAccount = new Account("" + getUserConfig().getClientUserId(), "org.telegram.messenger");
                    am.addAccountExplicitly(systemAccount, "", null);
                } catch (Exception ignore) {

                }
            }
        }
    }
    private boolean loadingContacts;
    private final Object loadContactsSync = new Object();
    public ArrayList<TLRPC.TL_contact> contacts = new ArrayList<>();
    public void readContacts() {
        synchronized (loadContactsSync) {
            if (loadingContacts) {
                return;
            }
            loadingContacts = true;
        }

        Utilities.stageQueue.postRunnable(() -> {
            if (!contacts.isEmpty() || contactsLoaded) {
                synchronized (loadContactsSync) {
                    loadingContacts = false;
                }
                return;
            }
            loadContacts(true, 0);
        });
    }

    public void loadContacts(boolean fromCache, final int hash) {
        synchronized (loadContactsSync) {
            loadingContacts = true;
        }
        if (fromCache) {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("load contacts from cache");
            }
            getMessagesStorage().getContacts();
        }
    }

    public void checkContacts() {
        Utilities.globalQueue.postRunnable(() -> {
            if (checkContactsInternal()) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("detected contacts change");
                }
                performSyncPhoneBook(getContactsCopy(contactsBook), true, false, true, false, true, false);
            }
        });
    }

    public HashMap<String, Contact> getContactsCopy(HashMap<String, Contact> original) {
        HashMap<String, Contact> ret = new HashMap<>();
        for (HashMap.Entry<String, Contact> entry : original.entrySet()) {
            Contact copyContact = new Contact();
            Contact originalContact = entry.getValue();
            copyContact.phoneDeleted.addAll(originalContact.phoneDeleted);
            copyContact.phones.addAll(originalContact.phones);
            copyContact.phoneTypes.addAll(originalContact.phoneTypes);
            copyContact.shortPhones.addAll(originalContact.shortPhones);
            copyContact.first_name = originalContact.first_name;
            copyContact.last_name = originalContact.last_name;
            copyContact.contact_id = originalContact.contact_id;
            copyContact.key = originalContact.key;
            ret.put(copyContact.key, copyContact);
        }
        return ret;
    }

}

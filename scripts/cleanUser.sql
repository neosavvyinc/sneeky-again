--To cleanup Philips db info:
delete from CONTACTS where OWNER_ID = 7
delete from SESSIONS where USERID = 7;
delete from USERS where id = 7;

--To cleanup Dave’s info on his Teat1 user
delete from CONVERSATIONS WHERE TO_USER = 3;
delete from CONVERSATIONS WHERE FROM_USER = 3;
delete from CONTACTS where OWNER_ID = 3;
delete from CONTACTS where CONTACT_ID = 3;
delete from USERS where ID=3;

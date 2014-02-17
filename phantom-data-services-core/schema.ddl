drop database phantom;
create database phantom;
use phantom;
create table USERS (ID BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,UUID VARCHAR(254) NOT NULL,EMAIL VARCHAR(256),PASSWORD VARCHAR(300),BIRTHDAY DATE,ACTIVE BOOLEAN NOT NULL,PHONE_NUMBER VARCHAR(254),STATUS VARCHAR(254) NOT NULL,INVITATION_COUNT INTEGER NOT NULL,SOUND_NOTIF BOOLEAN NOT NULL,NEW_PICTURE_NOTIF BOOLEAN NOT NULL);
create table CONVERSATIONS (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,TO_USER BIGINT NOT NULL,FROM_USER BIGINT NOT NULL,RECV_PHONE_NUMBER VARCHAR(254) NOT NULL,LAST_UPDATE_DATE TIMESTAMP(3) NOT NULL);
create table CONVERSATION_ITEMS (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,CONVERSATION_ID BIGINT NOT NULL,IMAGE_URL VARCHAR(254) NOT NULL,IMAGE_TEXT VARCHAR(254) NOT NULL,TO_USER BIGINT NOT NULL,FROM_USER BIGINT NOT NULL,IS_VIEWED BOOLEAN NOT NULL,CREATED_DATE TIMESTAMP(3) NOT NULL,TO_USER_DELETE BOOLEAN NOT NULL,FROM_USER_DELETE BOOLEAN NOT NULL);
create table CONTACTS (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,OWNER_ID BIGINT NOT NULL,CONTACT_ID BIGINT NOT NULL,TYPE VARCHAR(254) NOT NULL);
create unique index uniqueContact on CONTACTS (OWNER_ID,CONTACT_ID);
create table SESSIONS (SESSIONID VARCHAR(254) NOT NULL,USERID BIGINT NOT NULL,CREATED TIMESTAMP NOT NULL,LASTACCESSED TIMESTAMP NOT NULL,PUSH_NOTIFIER_TOKEN VARCHAR(254),PUSH_NOTIFIER_TYPE VARCHAR(254));
alter table CONVERSATIONS add constraint TO_USER_FK foreign key(TO_USER) references USERS(ID) on update NO ACTION on delete NO ACTION;
alter table CONVERSATIONS add constraint FROM_USER_FK foreign key(FROM_USER) references USERS(ID) on update NO ACTION on delete NO ACTION;
alter table CONVERSATION_ITEMS add constraint TO_CONV_USER_FK foreign key(TO_USER) references USERS(ID) on update NO ACTION on delete NO ACTION;
alter table CONVERSATION_ITEMS add constraint FROM_CONV_USER_FK foreign key(FROM_USER) references USERS(ID) on update NO ACTION on delete NO ACTION;
alter table CONVERSATION_ITEMS add constraint CONVERSATION_FK foreign key(CONVERSATION_ID) references CONVERSATIONS(id) on update NO ACTION on delete NO ACTION;
alter table CONTACTS add constraint CONTACT_FK foreign key(CONTACT_ID) references USERS(ID) on update NO ACTION on delete NO ACTION;
alter table CONTACTS add constraint OWNER_FK foreign key(OWNER_ID) references USERS(ID) on update NO ACTION on delete NO ACTION;
alter table SESSIONS add constraint USER_FK foreign key(USERID) references USERS(ID) on update NO ACTION on delete NO ACTION;

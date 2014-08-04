ALTER TABLE CONVERSATIONS DROP FOREIGN KEY TO_USER_FK
ALTER TABLE CONVERSATIONS DROP FOREIGN KEY FROM_USER_FK
ALTER TABLE CONVERSATION_ITEMS DROP FOREIGN KEY TO_CONV_USER_FK
ALTER TABLE CONVERSATION_ITEMS DROP FOREIGN KEY FROM_CONV_USER_FK
ALTER TABLE CONVERSATION_ITEMS DROP FOREIGN KEY CONVERSATION_FK
ALTER TABLE CONTACTS DROP FOREIGN KEY OWNER_FK
ALTER TABLE CONTACTS DROP FOREIGN KEY CONTACT_FK
ALTER TABLE PHOTOS DROP FOREIGN KEY PHOTO_CATEGORY_FK
ALTER TABLE SESSIONS DROP FOREIGN KEY USER_FK
drop table `USERS`
drop table `CONVERSATIONS`
drop table `CONVERSATION_ITEMS`
drop table `CONTACTS`
drop table `PHOTOS`
drop table `PHOTO_CATEGORIES`
drop table `SESSIONS`
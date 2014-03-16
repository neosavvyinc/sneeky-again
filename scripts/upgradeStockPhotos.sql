create table PHOTOS (ID BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,CATEGORY_ID BIGINT NOT NULL,IS_ACTIVE BOOLEAN NOT NULL,URL VARCHAR(254) NOT NULL);
create table PHOTO_CATEGORIES (ID BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,NAME VARCHAR(254) NOT NULL);
alter table PHOTOS add constraint PHOTO_CATEGORY_FK foreign key(CATEGORY_ID) references PHOTO_CATEGORIES(ID) on update NO ACTION on delete NO ACTION;

INSERT INTO PHOTO_CATEGORIES( NAME ) VALUES ( 'Backgrounds' );
INSERT INTO PHOTOS( CATEGORY_ID, IS_ACTIVE, URL ) VALUES (1, true, "https://s3.amazonaws.com/sneekyimages/stockImages/background_bigsmile%402x.png");
INSERT INTO PHOTOS( CATEGORY_ID, IS_ACTIVE, URL ) VALUES (1, true, "https://s3.amazonaws.com/sneekyimages/stockImages/background_cash%402x.png");
INSERT INTO PHOTOS( CATEGORY_ID, IS_ACTIVE, URL ) VALUES (1, true, "https://s3.amazonaws.com/sneekyimages/stockImages/background_exclaim%402x.png");
INSERT INTO PHOTOS( CATEGORY_ID, IS_ACTIVE, URL ) VALUES (1, true, "https://s3.amazonaws.com/sneekyimages/stockImages/background_frown%402x.png");
INSERT INTO PHOTOS( CATEGORY_ID, IS_ACTIVE, URL ) VALUES (1, true, "https://s3.amazonaws.com/sneekyimages/stockImages/background_hashtag%402x.png");
INSERT INTO PHOTOS( CATEGORY_ID, IS_ACTIVE, URL ) VALUES (1, true, "https://s3.amazonaws.com/sneekyimages/stockImages/background_heart%402x.png");
INSERT INTO PHOTOS( CATEGORY_ID, IS_ACTIVE, URL ) VALUES (1, true, "https://s3.amazonaws.com/sneekyimages/stockImages/background_question%402x.png");
INSERT INTO PHOTOS( CATEGORY_ID, IS_ACTIVE, URL ) VALUES (1, true, "https://s3.amazonaws.com/sneekyimages/stockImages/background_silent%402x.png");
INSERT INTO PHOTOS( CATEGORY_ID, IS_ACTIVE, URL ) VALUES (1, true, "https://s3.amazonaws.com/sneekyimages/stockImages/background_singularity%402x.png");
INSERT INTO PHOTOS( CATEGORY_ID, IS_ACTIVE, URL ) VALUES (1, true, "https://s3.amazonaws.com/sneekyimages/stockImages/background_smile%402x.png");
INSERT INTO PHOTOS( CATEGORY_ID, IS_ACTIVE, URL ) VALUES (1, true, "https://s3.amazonaws.com/sneekyimages/stockImages/background_winksmile%402x.png");
INSERT INTO PHOTOS( CATEGORY_ID, IS_ACTIVE, URL ) VALUES (1, true, "https://s3.amazonaws.com/sneekyimages/stockImages/background_winktongue%402x.png");
CREATE TABLE SPORT(
  sport_id BIGSERIAL PRIMARY KEY NOT NULL,
  label VARCHAR (20) NOT NULL,
  description VARCHAR (140) NOT NULL ,
  image_url VARCHAR (1024), /* TODO: Look at URL datatype */
  min_players INT,
  max_players INT,
  wait_list_amount INT,
  active BOOLEAN NOT NULL , /*soft deletion flag*/
  CONSTRAINT sport_label_unique UNIQUE (label)
);

CREATE TABLE CURRANT_USER(
  currant_user_id BIGSERIAL PRIMARY KEY NOT NULL,
  email_address VARCHAR(50) NOT NULL, /* TODO: should determine max value of an email address */
  password VARCHAR (256) NOT NULL,
  account_status VARCHAR (20) NOT NULL, /*status can represent stuff like non verified, reset password, etc */
  subscriber_type VARCHAR (20) NOT NULL, /* TODO: decide if this should be a foreign key to another table for "paid" "subscribed" etc...*/
  active BOOLEAN NOT NULL, /* active represents whether or not the user is active..might be able to tie into status */
  CONSTRAINT user_email_address_unique UNIQUE (email_address)
);
/*Where to break user and profile? */
CREATE TABLE PROFILE(
  profile_id BIGSERIAL PRIMARY KEY NOT NULL,
  currant_user_id BIGINT NOT NULL,
  source VARCHAR (10) NOT NULL, /*currant, facebook, etc..not sure how this will play out since not sure the rules of editting a profile if it comes from fb */
  source_identifier VARCHAR (256) NOT NULL, /*TODO: Decide if this is the right data type */
  first_name VARCHAR (20),
  last_name VARCHAR(20),
  image_url VARCHAR (50),
  bio VARCHAR (140), /*blurb*/
  city VARCHAR (50),
  state VARCHAR(50),
  country VARCHAR (50),
  profile_level VARCHAR (10) NOT NULL, /*elite, standard, etc*/
  preferred_time VARCHAR (10), /*preferred time to play */
  location_enabled BOOLEAN NOT NULL DEFAULT true ,
  new_game_notification BOOLEAN NOT NULL DEFAULT true,
  friend_activity_notification BOOLEAN NOT NULL DEFAULT true,
  news_promotions_notification BOOLEAN NOT NULL DEFAULT true,
  payment_receipt VARCHAR(100),
  FOREIGN KEY (currant_user_id) REFERENCES currant_user (currant_user_id)
);

CREATE TABLE PUSH_NOTIFICATION_IDENTIFIER(
  push_notification_identifier_id BIGSERIAL PRIMARY KEY NOT NULL,
  identifier UUID  NOT NULL,
  platform VARCHAR(10) NOT NULL,
  profile_id BIGINT NOT NULL,
  FOREIGN KEY (profile_id) REFERENCES PROFILE (profile_id),
  CONSTRAINT push_notification_identifier_unique UNIQUE (identifier, platform, profile_id)
);

CREATE TABLE PROFILE_SPORT(
  profile_id BIGINT NOT NULL,
  sport_id BIGINT NOT NULL ,
  sort_order INT NOT NULL,
  FOREIGN KEY (profile_id) REFERENCES PROFILE(profile_id),
  FOREIGN KEY (sport_id) REFERENCES SPORT(sport_id),
  CONSTRAINT profile_sport_unique UNIQUE (profile_id, sport_id)
);

CREATE TABLE EQUIPMENT(
  equipment_id BIGSERIAL PRIMARY KEY NOT NULL,
  label VARCHAR (30) NOT NULL,
  description VARCHAR (100),
  image_url VARCHAR (50),
  active BOOL NOT NULL ,
  CONSTRAINT equipment_label_unique UNIQUE (label)
);

CREATE TABLE SPORT_EQUIPMENT(
  sport_id BIGINT NOT NULL,
  equipment_id BIGINT NOT NULL,
  sort_order INT NOT NULL,
  FOREIGN KEY (sport_id) REFERENCES SPORT(sport_id),
  FOREIGN KEY (equipment_id) REFERENCES EQUIPMENT(EQUIPMENT_ID),
  CONSTRAINT sport_equipment_unique UNIQUE (sport_id, equipment_id)
);
/*this is not scalable..neo4j? */
CREATE TABLE CONNECTION (
  source_profile_id BIGINT NOT NULL,
  target_profile_id BIGINT NOT NULL,
  status VARCHAR(10) NOT NULL, /*friends, blocked, invited, unfriended */
  FOREIGN KEY (source_profile_id) REFERENCES PROFILE(profile_id),
  FOREIGN KEY (target_profile_id) REFERENCES PROFILE(profile_id),
  CONSTRAINT connection_unique UNIQUE (SOURCE_PROFILE_ID, TARGET_PROFILE_ID)
);

CREATE TABLE CLUB (
  club_id BIGSERIAL PRIMARY KEY NOT NULL,
  label VARCHAR (30) NOT NULL,
  description VARCHAR(100),
  location VARCHAR(100), /*is this ok? */
  gps_coordinates VARCHAR(50), /* TODO: Use PostgreSQL datatype for GPS coordinates */
  club_icon BIGINT,
  club_icon_back_color INT,
  club_icon_front_color INT,
  visibility VARCHAR(10) NOT NULL,
  open_enrollment BOOLEAN NOT NULL,/*approval flag*/
  status VARCHAR(10) NOT NULL, /* active, inactive, etc... */
  byof BOOLEAN NOT NULL, /*bring your own friends(invitation for friends - this flag can not be set on a public game */
  CONSTRAINT club_label_unique UNIQUE (label)
);

CREATE TABLE CLUB_ICON (
  club_icon_id BIGSERIAL PRIMARY KEY NOT NULL,
  image_url VARCHAR(50) NOT NULL,
  locked BOOLEAN NOT NULL default(false),
  CONSTRAINT club_icon_image_url_unique UNIQUE (image_url)
);

CREATE TABLE CLUB_IMAGE (
  club_id BIGINT NOT NULL,
  image_url VARCHAR(50) NOT NULL,
  sort_order INT NOT NULL,
  FOREIGN KEY (club_id) REFERENCES CLUB(club_id),
  CONSTRAINT club_image_unique UNIQUE (club_id, image_url, sort_order)
);

CREATE TABLE CLUB_PROFILE_CONNECTION (
  club_id BIGINT NOT NULL,
  profile_id BIGINT NOT NULL,
  status VARCHAR(10) NOT NULL, /*friends blocked, founder, invited, etc*/
  FOREIGN KEY (club_id) REFERENCES CLUB(club_id),
  FOREIGN KEY (profile_id) REFERENCES PROFILE(profile_id),
  CONSTRAINT club_profile_connection_unique UNIQUE (club_id, profile_id)
);

CREATE TABLE CLUB_SPORT (
  club_id BIGINT NOT NULL,
  sport_id BIGINT NOT NULL,
  sort_order INT NOT NULL,
  FOREIGN KEY (club_id) REFERENCES CLUB(club_id),
  FOREIGN KEY (sport_id) REFERENCES SPORT(sport_id),
  CONSTRAINT club_sport_unique UNIQUE (club_id, sport_id, sort_order)
);

CREATE TABLE CLUB_WALL_POST (
  club_wall_post_id BIGSERIAL PRIMARY KEY NOT NULL,
  author_profile_id BIGINT NOT NULL,
  club_id BIGINT NOT NULL,
  text VARCHAR (200) NOT NULL,
  image_url VARCHAR (50),
  created TIMESTAMP NOT NULL,
  parent_post_id BIGINT,
  FOREIGN KEY (author_profile_id) REFERENCES PROFILE(profile_id),
  FOREIGN KEY (club_id) references CLUB(club_id)
);
/*how does club on club work?  You can imagine there being a game between 2 rival clubs*/
CREATE TABLE GAME (
  game_id BIGSERIAL PRIMARY KEY NOT NULL,
  sport_id BIGINT NOT NULL,
  visibility VARCHAR(10) NOT NULL,
  /*if created by a club, or has club level access only(effectively the same thing? can a person invite a club if they are not the manager of the club?*/
  club_id BIGINT,
  location VARCHAR(100), /* TODO: Make this consistent with other Location data fields*/
  gps_coordinates VARCHAR(50), /* TODO: use the Postgresql datatype */
  byof BOOLEAN NOT NULL,
  game_time TIMESTAMP NOT NULL,
  size INT NOT NULL,
  intensity VARCHAR(10) NOT NULL,
  description VARCHAR(200) NOT NULL,
  image_url VARCHAR (1024), /* TODO: Look at URL datatype */
  status VARCHAR(10) NOT NULL, /*scheduled, in progress, cancelled, finished */
  waitlist_strategy VARCHAR(5) NOT NULL default('blast'),
  FOREIGN KEY (sport_id) REFERENCES SPORT(sport_id),
  FOREIGN KEY (club_id) REFERENCES CLUB(club_id)
);

CREATE TABLE GAME_PROFILE_CONNECTION (
  game_id BIGINT NOT NULL,
  profile_id BIGINT NOT NULL,
  status VARCHAR(10) NOT NULL, /*creator, invited, accepted, blocked,waitlisted */
  FOREIGN KEY (game_id) REFERENCES GAME(game_id),
  FOREIGN KEY (profile_id) REFERENCES PROFILE(profile_id),
  CONSTRAINT game_profile_connection_unique UNIQUE (game_id, profile_id)
);

CREATE TABLE GAME_EQUIPMENT (
  game_id BIGINT NOT NULL,
  equipment_id BIGINT NOT NULL,
  amount INT,
  claimed BOOL,
  claimed_by_profile_id BIGINT,
  sort_order INT NOT NULL,
  FOREIGN KEY (game_id) REFERENCES GAME(game_id),
  FOREIGN KEY (equipment_id) REFERENCES EQUIPMENT(equipment_id),
  FOREIGN KEY (claimed_by_profile_id) REFERENCES PROFILE(profile_id),
  CONSTRAINT game_equipment_unique UNIQUE (game_id, equipment_id)
);

CREATE TABLE GAME_PROFILE_STARRED (
  game_id BIGINT NOT NULL,
  profile_ID BIGINT NOT NULL,
  FOREIGN KEY (game_id) REFERENCES GAME(game_id),
  FOREIGN KEY (profile_id) REFERENCES PROFILE(profile_id),
  CONSTRAINT game_profile_starred_unique UNIQUE (game_id, profile_id)
);

CREATE TABLE GAME_IMAGE (
  game_id BIGINT NOT NULL,
  image_url VARCHAR(50) NOT NULL,
  sort_order INT NOT NULL,
  FOREIGN KEY (game_id) REFERENCES GAME(game_id),
  CONSTRAINT game_image_unique UNIQUE (game_id, image_url)
);












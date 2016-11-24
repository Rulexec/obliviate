package ruliov.obliviate.db.migrations

fun main(args: Array<String>) = executeSQL("""
CREATE OR REPLACE FUNCTION random_bytea(bytea_length integer)
RETURNS bytea AS ${'$'}body${'$'}
    SELECT decode(string_agg(lpad(to_hex(width_bucket(random(), 0, 1, 256)-1),2,'0') ,''), 'hex')
    FROM generate_series(1, $1);
${'$'}body${'$'}
LANGUAGE 'sql'
VOLATILE
SET search_path = 'pg_catalog';

CREATE TABLE "otherTranslations" (
    "wordId" integer NOT NULL,
    "translationId" uuid NOT NULL
) WITH ( OIDS=FALSE);

CREATE TABLE sessions (
    id bytea NOT NULL DEFAULT random_bytea(16),
    "userId" integer,
    "expiresAt" timestamp without time zone DEFAULT (timezone('UTC'::text, now()) + '14 days'::interval) NOT NULL
) WITH ( OIDS=FALSE);

CREATE TABLE translations (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    text text NOT NULL,
    "wordId" integer NOT NULL,
    "partOfSpeech" smallint DEFAULT 0 NOT NULL
) WITH ( OIDS=FALSE);

CREATE TABLE users (
    id integer NOT NULL
) WITH ( OIDS=FALSE);

CREATE TABLE "usersVk" (
    "userId" integer NOT NULL,
    "vkUserId" integer NOT NULL,
    "accessToken" character varying(128) NOT NULL,
    "expiresAt" timestamp without time zone NOT NULL
) WITH ( OIDS=FALSE);

CREATE SEQUENCE users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE users_id_seq OWNED BY users.id;

CREATE SEQUENCE words_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

CREATE TABLE words (
    id integer DEFAULT nextval('words_id_seq'::regclass) NOT NULL,
    text character varying(32) NOT NULL,
    "partOfSpeech" smallint DEFAULT 0 NOT NULL,
  "ownerId" integer NOT NULL DEFAULT 0,
  CONSTRAINT "words_ownerId_fkey" FOREIGN KEY ("ownerId")
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
) WITH ( OIDS=FALSE);

ALTER TABLE ONLY users ALTER COLUMN id SET DEFAULT nextval('users_id_seq'::regclass);
ALTER TABLE ONLY "otherTranslations"
    ADD CONSTRAINT "otherTranslations_pkey" PRIMARY KEY ("wordId", "translationId");
ALTER TABLE ONLY sessions
    ADD CONSTRAINT sessions_pkey PRIMARY KEY (id);
ALTER TABLE ONLY translations
    ADD CONSTRAINT translations_pkey PRIMARY KEY (id);
ALTER TABLE ONLY translations
    ADD CONSTRAINT "translations_wordId_key" UNIQUE ("wordId");
ALTER TABLE ONLY "usersVk"
    ADD CONSTRAINT "usersVk_pkey" PRIMARY KEY ("userId", "vkUserId");
ALTER TABLE ONLY "usersVk"
    ADD CONSTRAINT "usersVk_vkUserId_key" UNIQUE ("vkUserId");
ALTER TABLE ONLY words
    ADD CONSTRAINT words_pkey PRIMARY KEY (id);
CREATE INDEX "usersVk_vkUserId_idx" ON "usersVk" USING hash ("vkUserId");
ALTER TABLE ONLY translations
    ADD CONSTRAINT "translations_wordId_fkey" FOREIGN KEY ("wordId") REFERENCES words(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ONLY "usersVk"
    ADD CONSTRAINT "usersVk_userId_fkey" FOREIGN KEY ("userId") REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE;

CREATE INDEX words_text_idx
  ON words
  USING btree
  (text COLLATE pg_catalog."default");

INSERT INTO users (id) VALUES (0);
""")

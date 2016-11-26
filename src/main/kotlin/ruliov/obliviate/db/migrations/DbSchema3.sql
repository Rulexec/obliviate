SET default_with_oids = false;
CREATE TABLE "otherTranslations" (
    "wordId" integer NOT NULL,
    "translationId" uuid NOT NULL
);
CREATE TABLE sessions (
    "userId" bigint,
    id character(32) NOT NULL,
    "expiresAt" bigint DEFAULT ((date_part('epoch'::text, (timezone('UTC'::text, now()) + '14 days'::interval)) * (1000)::double precision))::bigint NOT NULL
);
CREATE TABLE translations (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    text text NOT NULL,
    "wordId" integer NOT NULL,
    "partOfSpeech" smallint DEFAULT 0 NOT NULL
);
CREATE TABLE users (
    id bigint NOT NULL
);
CREATE TABLE "usersVk" (
    "userId" bigint NOT NULL,
    "vkUserId" bigint NOT NULL,
    "accessToken" character varying(128) NOT NULL,
    "expiresAt" bigint DEFAULT ((date_part('epoch'::text, (timezone('UTC'::text, now()) + '12:00:00'::interval)) * (1000)::double precision))::bigint NOT NULL
);
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
CREATE TABLE words (
    id integer DEFAULT nextval('words_id_seq'::regclass) NOT NULL,
    text character varying(32) NOT NULL,
    "partOfSpeech" smallint DEFAULT 0 NOT NULL,
    "ownerId" integer DEFAULT 0 NOT NULL
);
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
ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);
ALTER TABLE ONLY words
    ADD CONSTRAINT words_pkey PRIMARY KEY (id);
CREATE INDEX "usersVk_vkUserId_idx" ON "usersVk" USING hash ("vkUserId");
CREATE INDEX words_text_idx ON words USING btree (text);
ALTER TABLE ONLY translations
    ADD CONSTRAINT "translations_wordId_fkey" FOREIGN KEY ("wordId") REFERENCES words(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ONLY "usersVk"
    ADD CONSTRAINT "usersVk_userId_fkey" FOREIGN KEY ("userId") REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ONLY words
    ADD CONSTRAINT "words_ownerId_fkey" FOREIGN KEY ("ownerId") REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE;

INSERT INTO users (id) VALUES (0);
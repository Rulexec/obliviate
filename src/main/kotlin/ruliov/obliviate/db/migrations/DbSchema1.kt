@file:JvmName("DbSchema1")

package ruliov.obliviate.db.migrations

fun main(args: Array<String>) = executeSQL("""
CREATE TABLE translations
(
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  text text NOT NULL,
  CONSTRAINT translations_pkey PRIMARY KEY (id)
) WITH ( OIDS=FALSE );

CREATE SEQUENCE words_id_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 0
  CACHE 1;

CREATE TABLE words
(
  id integer NOT NULL DEFAULT nextval('words_id_seq'::regclass),
  word character varying(32) NOT NULL,
  "translationId" uuid NOT NULL,
  CONSTRAINT words_pkey PRIMARY KEY (id),
  CONSTRAINT "words_translationId_fkey" FOREIGN KEY ("translationId")
      REFERENCES public.translations (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT
) WITH ( OIDS=FALSE );
""")
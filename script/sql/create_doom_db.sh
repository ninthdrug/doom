#!/bin/sh
echo "create doom user"
sudo -u postgres createuser \
    --no-superuser \
    --no-createdb \
    --no-createrole \
    --pwprompt \
    doom

echo "create doomreader user"
sudo -u postgres createuser \
    --no-superuser \
    --no-createdb \
    --no-createrole \
    --pwprompt \
    doomreader

echo "create doom database"
sudo -u postgres createdb --owner doom doom

echo "create tables"
psql doom doom <<EndCreateTables
CREATE TABLE machines (
    machinename VARCHAR(40) PRIMARY KEY, 
    address VARCHAR(40),
    os_user VARCHAR(10)
);

CREATE TABLE domains (
    domainname VARCHAR(40) PRIMARY KEY, 
    domaintype VARCHAR(10), 
    domaingroup VARCHAR(40),
    project VARCHAR(20),
    env VARCHAR(10),
    java_vendor VARCHAR(10), 
    java_home VARCHAR(64), 
    bea_home VARCHAR(64),
    wl_home VARCHAR(64),
    wl_version VARCHAR(20),
    osb_home VARCHAR(64),
    osb_version VARCHAR(20),
    alsb_home VARCHAR(64),
    alsb_version VARCHAR(20)
);

CREATE TABLE servers (
    servername VARCHAR(40) PRIMARY KEY,
    servertype VARCHAR(20),
    domainname VARCHAR(40),
    clustername VARCHAR(40) default '',
    machinename VARCHAR(32) default '',
    address varchar(40),
    port INTEGER,
    jmxport INTEGER,
    blocked BOOLEAN default 'f'
);

CREATE TABLE audit (
    id SERIAL PRIMARY KEY,
    date  timestamp,
    userid VARCHAR(10),
    servername VARCHAR(40),
    status VARCHAR(32),
    action VARCHAR(32),
    enddate timestamp
);

CREATE TABLE users (
    userid VARCHAR(10) PRIMARY KEY,
    username VARCHAR(50),
    email VARCHAR(50),
    enabled BOOLEAN
);

CREATE TABLE groups (
    groupname VARCHAR(20) PRIMARY KEY
);

CREATE TABLE groupmembers (
    groupname VARCHAR(20),
    userid VARCHAR(10)
); 

CREATE TABLE permissions (
    id SERIAL PRIMARY KEY,
    groupname VARCHAR(20),
    project VARCHAR(20),
    env VARCHAR(10),
    action VARCHAR(20)
);

CREATE TABLE credentials (
    id SERIAL PRIMARY KEY,
    realmtype VARCHAR(20),
    realm VARCHAR(20),
    principal VARCHAR(20),
    password VARCHAR(100)
);

EndCreateTables

echo "grant read access to doomreader"
psql doom doom <<EndGrant
grant select on table machines to doomreader;
grant select on table domains to doomreader;
grant select on table servers to doomreader;
grant select on table audit to doomreader;
grant select on table users to doomreader;
grant select on table groups to doomreader;
grant select on table groupmembers to doomreader;
grant select on table permissions to doomreader;
grant select on table credentials to doomreader;

EndGrant

echo "initialize permissions table"
psql doom doom <<EndPermissions
INSERT INTO permissions (groupname, project, env, action) VALUES ( 'gods', '', '', '');
INSERT INTO permissions (groupname, project, env, action) VALUES ( 'admins', '', '', '');
INSERT INTO permissions (groupname, project, env, action) VALUES ( 'testers', '', 'tst', '');
INSERT INTO permissions (groupname, project, env, action) VALUES ( 'developers', '', 'dev', '');

EndPermissions

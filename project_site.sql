create table project_site ( proj_id int not null, geom geometry(POINT,22717) not null, constraint fk_project foreign key(proj_id) references project(proj_id) );

# Hibernate gets an ID
create table project_site ( id int primary key, proj_id int not null, geom geometry(POINT,22717) not null, constraint fk_project foreign key(proj_id) references project(proj_id) );

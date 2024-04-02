create table grabber_service.post (
	id serial primary key,
	name varchar(255),
	text text,
	link varchar(255) unique,
	created timestamp without time zone
)
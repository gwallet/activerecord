CREATE TABLE ContactGroup (
  id int primary key auto_increment,
  name varchar(256)
);

ALTER TABLE Contact
ADD groupId int;

ALTER TABLE Contact
ADD FOREIGN KEY (groupId) REFERENCES ContactGroup(id);
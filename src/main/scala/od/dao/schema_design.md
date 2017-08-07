# Schema Explanation for OfflineDownloader
The database name for OfflineDownloader is `od`, in this version, there will only be a single table called `offlinefile`

Columns in table `offlinefile` :
* id
* hash
* size
* create_time

Here comes the script that helps you to create the database on the fly
```
CREATE DATABASE IF NOT EXISTS od;
USE od;
CREATE TABLE `offlinefile` (`id` INTEGER AUTO_INCREMENT PRIMARY KEY,
 `hash` CHAR(40) NOT NULL,
 `size` INTEGER,
 `create_time` TIMESTAMP DEFAULT NOW());
```




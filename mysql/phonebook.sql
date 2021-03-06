-- MySQL Script generated by MySQL Workbench
-- 09/14/16 12:40:48
-- Model: New Model    Version: 1.0
-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema phonebook
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema phonebook
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `phonebook` DEFAULT CHARACTER SET utf8 ;
USE `phonebook` ;

-- -----------------------------------------------------
-- Table `phonebook`.`contact`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `phonebook`.`contact` (
  `contact_id` INT NOT NULL AUTO_INCREMENT,
  `first_name` VARCHAR(45) NULL,
  `last_name` VARCHAR(45) NULL,
  PRIMARY KEY (`contact_id`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `phonebook`.`phone_type`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `phonebook`.`phone_type` (
  `phone_type_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `phone_type_name` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`phone_type_id`),
  UNIQUE INDEX `type_name_UNIQUE` (`phone_type_name` ASC))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `phonebook`.`phone_mask`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `phonebook`.`phone_mask` (
  `phone_mask_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `phone_mask_view` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`phone_mask_id`),
  UNIQUE INDEX `mask_UNIQUE` (`phone_mask_view` ASC))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `phonebook`.`phone_number`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `phonebook`.`phone_number` (
  `phone_number_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `phone_number` VARCHAR(45) NOT NULL,
  `phone_type_id` INT UNSIGNED NOT NULL,
  `phone_mask_id` INT UNSIGNED NOT NULL,
  `contact_id` INT NOT NULL,
  PRIMARY KEY (`phone_number_id`, `contact_id`),
  UNIQUE INDEX `phone_number_UNIQUE` (`phone_number` ASC),
  INDEX `fk_phone_numbers_phone_types1_idx` (`phone_type_id` ASC),
  INDEX `fk_phone_numbers_phone_masks1_idx` (`phone_mask_id` ASC),
  INDEX `fk_phone_number_contact1_idx` (`contact_id` ASC),
  CONSTRAINT `fk_phone_numbers_phone_types`
    FOREIGN KEY (`phone_type_id`)
    REFERENCES `phonebook`.`phone_type` (`phone_type_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_phone_numbers_phone_masks`
    FOREIGN KEY (`phone_mask_id`)
    REFERENCES `phonebook`.`phone_mask` (`phone_mask_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_phone_number_contact1`
    FOREIGN KEY (`contact_id`)
    REFERENCES `phonebook`.`contact` (`contact_id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

DROP USER restapi;
FLUSH PRIVILEGES;
CREATE USER 'restapi' IDENTIFIED BY 'restapi';


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

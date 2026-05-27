package com.say5.equipfleet.service;

/** Raised when a referenced domain entity does not exist. */
public class NotFoundException extends RuntimeException {
  public NotFoundException(String message) {
    super(message);
  }
}

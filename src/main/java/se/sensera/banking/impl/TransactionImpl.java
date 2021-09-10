package se.sensera.banking.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import se.sensera.banking.Account;
import se.sensera.banking.Transaction;
import se.sensera.banking.User;

import java.util.Date;

@Data
@AllArgsConstructor
public class TransactionImpl implements Transaction {
   private final String id;
   private Date created;
   private User user;
   private Account account;
   private double amount;
}

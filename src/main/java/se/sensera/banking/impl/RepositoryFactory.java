package se.sensera.banking.impl;

public class RepositoryFactory extends RepositoryImpl {

    public static Object createRepository(String name) {
        switch (name) {
           case "UserRepository" -> {return new UsersRepositoryImpl();}
            case "AccountRepository" -> {return new AccountsRepositoryImpl();}
            case "TransactionRepository" -> {return new TransactionsRepositoryImpl();}
        }
        throw new IllegalArgumentException("doesn't exist " + name);
    }
}

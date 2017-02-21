package com.accenture.cordova.plugin.sso;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.Bundle;

import java.io.IOException;

/**
 * Created by joshualamkin on 9/3/15.
 */
public class AccountManger {

    AccountManager manager = null;

    public AccountManger(Context context) {
        super();
        manager = AccountManager.get(context);
    }

    public Account[] getAccountsByType(String accountType) {
        return manager.getAccountsByType(accountType);
    }

    public Account addAccountExplicitly(String username, String password, String accountType) {

        Account account = new Account(username, accountType);

        Bundle userdata = new Bundle();
        if (!manager.addAccountExplicitly(account, password, userdata)) {
            return null;
        }

        return account;
    }

    public boolean removeAccount(Account account) {
        if (account == null) {
            return false;
        }

        AccountManagerFuture<Boolean> future = manager.removeAccount(account, null, null);
        try {
            if (future.getResult()) {
                return true;
            } else {
                return false;
            }
        } catch (OperationCanceledException e) {
            return false;
        } catch (AuthenticatorException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean setAuthToken(Account account, String authTokenType, String authToken) {
        if (account == null) {
            return true;
        }

        manager.setAuthToken(account, authTokenType, authToken);
        return true;
    }

    public void invalidateAuthToken(String accountType, String authToken) {
        manager.invalidateAuthToken(accountType, authToken);
    }

    public String peekAuthToken(Account account, String authTokenType) {
        if (account == null) {
            return null;
        }

        return manager.peekAuthToken(account, authTokenType);
    }
}

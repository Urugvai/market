package org.morozov.market.util;

/**
 * Created by Morozov on 5/22/2017.
 */
public interface DialogHolder {

    String WELCOME_SPEECH = "Welcome in our market! Please, use 'login %user name%' command for starting.\n\r";

    String INCORRECT_LOGIN = "Wrong login command or imputed login is incorrect now. Please try another.\n\r";

    String UNKNOWN_COMMAND = "Unknown command, please try again!\n\r";

    String COMMAND_LIST = "Available commands:\n\r'logout'\n\r'viewshop'\n\r'myinfo'\n\r'buy %item name%'\n\r'sell %item name%'\n\r";

    String ITEM_ARE_NOT_FOUND = "Imputed item aren't found in the shop!\n\r";

    String ITEM_ARE_NOT_FOUND_FOR_SELLING = "Imputed item aren't found for selling!\n\r";

    String NOT_ENOUGH_FUNDS = "Not enough funds for buying!\n\r";

    String SUCCESSFUL_OPERATION = "Successful operation!\n\r";

    String USER_WAS_DELETED = "User was deleted!\n\r";
}

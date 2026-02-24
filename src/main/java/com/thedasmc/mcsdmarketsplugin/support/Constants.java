package com.thedasmc.mcsdmarketsplugin.support;

import java.time.Duration;

public class Constants {

    public static final String BASE_COMMAND = "markets|mcsd";

    public static final String CHECK_COMMAND_PERMISSION = "mcsd.check";
    public static final String BUY_COMMAND_PERMISSION = "mcsd.buy";
    public static final String WITHDRAW_PERMISSION = "mcsd.withdraw";
    public static final String VIEW_COMMAND_PERMISSION = "mcsd.view";
    public static final String PRICE_HISTORY_COMMAND_PERMISSION = "mcsd.pricehistory";
    public static final String PORTFOLIO_COMMAND_PERMISSION = "mcsd.portfolio";
    public static final String LIMIT_ORDER_PERMISSION = "mcsd.limitorder";

    public static final Duration MAX_SYNC_THREAD_WAIT = Duration.ofSeconds(10);

}

package com.thedasmc.mcsdmarketsplugin.support;

import java.time.Duration;

public class Constants {

    public static final String BASE_COMMAND = "markets|mcsd";

    public static final String CHECK_COMMAND_PERMISSION = "mcsd.check";
    public static final String BUY_COMMAND_PERMISSION = "mcsd.buy";
    public static final String WITHDRAW_CONTRACT_PERMISSION = "mcsd.contract.withdraw";
    public static final String VIEW_COMMAND_PERMISSION = "mcsd.view";

    public static final Duration MAX_SYN_THREAD_WAIT = Duration.ofSeconds(10);

}

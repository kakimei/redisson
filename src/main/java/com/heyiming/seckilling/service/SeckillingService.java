package com.heyiming.seckilling.service;

import java.util.List;

public interface SeckillingService {

    void prepareInventory();

    boolean reserve(String member);

    List<String> getSuccessMember();
}

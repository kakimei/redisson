package com.heyiming.seckilling.service;

import java.util.List;

public interface SeckillingService {

    void prepareInventory();

    boolean reserve(List<String> member, String itemKey);

    List<String> getSuccessMember(String itemKey);
}

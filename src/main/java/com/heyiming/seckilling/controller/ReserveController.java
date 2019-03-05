package com.heyiming.seckilling.controller;

import com.heyiming.seckilling.service.SeckillingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
public class ReserveController {

    @Autowired
    private SeckillingService seckillingService;

    @RequestMapping(path = "/reserve", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody boolean reserve(@RequestBody String member){
        return seckillingService.reserve(member);
    }

    @RequestMapping(path = "/prepare", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody boolean prepare(){
        seckillingService.prepareInventory();
        return true;
    }

    @RequestMapping(path = "/members", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody List<String> getAllMembers(){
        return seckillingService.getSuccessMember();
    }
}

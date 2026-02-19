package com.academy.controller;

import com.academy.service.DatabaseBackupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    DatabaseBackupService databaseBackupService;
    @GetMapping("/test-backup")
    public String testBackup() {
        databaseBackupService.performBackup();
        return "redirect:/dashboard?backup=success";
    }

}



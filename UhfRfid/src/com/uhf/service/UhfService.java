package com.uhf.service;

import java.sql.Connection;
import java.util.Scanner;

import com.uhf.configuration.ReaderConfigDao;
import com.uhf.dao.TagDao;
import com.uhf.dbconn.Database;
import com.uhf.utils.ServiceUtils;

public class UhfService {

    private final TagDao tagDao;
    private final ReaderConfigDao configDao;

    public UhfService(TagDao dao, ReaderConfigDao config) {
        this.tagDao = dao;
        this.configDao = config;
    }

    public void readMode(Scanner in) {
        ServiceUtils.readMode(in, tagDao);
    }

    public void writeMode(Scanner in) {
        System.out.println("-- Write AutoGenerate Mode -- (type q at the tag list to go back to the menu)");
        
        try (Connection connection = Database.getInstance().getConnection("HQ")) {

        	connection.setAutoCommit(false);
            while (ServiceUtils.writeOnce(in, tagDao, connection)) {
                System.out.println();
                System.out.println("-- Write AutoGenerate Mode -- ready for the next tag --");
            }
        } catch (Exception e) {
        	e.printStackTrace();
		}
    }

    public void readerSettings(Scanner in) {
        ServiceUtils.readerSettings(in, configDao);
    }

    public void writeManualMode(Scanner in) {
        System.out.println("-- Write Manual Mode -- (type q at the tag list to go back to the menu)");

        try (Connection connection = Database.getInstance().getConnection("HQ")) {

            while (ServiceUtils.writeManualOnce(in, tagDao, connection)) {
                System.out.println();
                System.out.println("-- Write Manual Mode -- ready for the next tag --");
            }
        } catch (Exception e) {
        	e.printStackTrace();
		}

        	
    }
}
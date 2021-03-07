/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.astronlab.ngenhttplib.utils;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * @author omar
 */
public class ReverseHostLookUp {

    private final String Ip;

    public ReverseHostLookUp(String ip) {
        this.Ip = ip;
    }

    public String getHostName() {
        String hostName = "";
        try {

            Hashtable env = new Hashtable();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            DirContext ctx = new InitialDirContext(env);

            Attributes attrs = ctx.getAttributes(getArpaName(), new String[]{"PTR"});
            String divider = "";
            if (attrs.size() > 1) {
                divider = "|";
            }

            for (NamingEnumeration ae = attrs.getAll(); ae.hasMoreElements(); ) {

                Attribute attr = (Attribute) ae.next();

                String attrId = attr.getID();

                for (Enumeration vals = attr.getAll(); vals.hasMoreElements(); ) {
                    hostName += divider + vals.nextElement();
                }

            }
            ctx.close();

        } catch (Exception e) {
            //ConsoleService.info("Exception in reverse DNS: ip: "+Ip+", "+e.getMessage());
            return Ip;
        }
        return hostName;
    }

    private String getArpaName() {
        String arpaName = "in-addr.arpa";
        String[] parts = Ip.split("\\.");
        int len = parts.length;
        for (int i = 0; i < len; i++) {
            arpaName = parts[i] + "." + arpaName;
        }
        return arpaName;
    }
}

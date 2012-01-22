package org.ovirt.engine.core.bll.adbroker;

public enum LdapVendorNameEnum {
    IPAVendorName("389 Project"),
    RHDSVendorName("Red Hat");

    private String name;

    private LdapVendorNameEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

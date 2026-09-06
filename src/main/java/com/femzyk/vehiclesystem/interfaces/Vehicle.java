package com.femzyk.vehiclesystem.interfaces;

import java.io.Serializable;

/** Legacy shadow interface (see package-info of com.femzyk.vehiclesystem). */
public interface Vehicle extends Serializable {
    String getMake();
    String getModel();
    int getYear();
    String getRenterName();
    String getRenterPhone();
    /** Legacy type-specific attribute rendered as text. */
    String legacyDetail();
}

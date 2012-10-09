package jj.appconnector;

import processing.core.PImage;

/*
** Wrapper class for storing the value of a publication
 ** including several helper casting classes
 */


public class AppData {

  private Object value = null;

  public void setValue(Object val) {
    this.value = (val != null && !val.equals(null)) ? val : null;
  }

  public Object getValue() {
    return this.value;
  }

  // cast to String
  @Override
public String toString() {
    if (this.value == null) return "";
    if (isAssignable(String.class)) {
      return String.class.cast(this.value);
    }

    return this.value.toString();
  }

  // cast to boolean
  public boolean toBoolean() {
    if (this.value == null) return false;
    if (isAssignable(Boolean.class)) {
      return Boolean.class.cast(this.value).booleanValue();
    }

    String s = toString();
    if (isNumeric(s)) {
      return Double.parseDouble(s) > 0 ? true : false;
    }

    return Boolean.valueOf(s).booleanValue();
  }

  // cast to int
  public int toInt() {
    if (this.value == null) return 0;
    if (isAssignable(Integer.class)) {
      return Integer.class.cast(this.value).intValue();
    }

    String s = toString();
    if (isNumeric(s)) {
      return (int) Double.parseDouble(s);
    }

    return s.toUpperCase().equals("TRUE") ? 1 : 0;
  }

  // cast to double
  public double toDouble() {
    if (this.value == null) return 0;
    if (isAssignable(Double.class)) {
      return Double.class.cast(this.value).doubleValue();
    }

    String s = toString();
    if (isNumeric(s)) {
      return Double.parseDouble(s);
    }

    return s.toUpperCase().equals("TRUE") ? 1 : 0;
  }

  // cast to float
  public float toFloat() {
    if (this.value == null) return 0;
    if (isAssignable(Float.class)) {
      return Float.class.cast(this.value).floatValue();
    }

    String s = toString();
    float f = 0;
    try {
      f = Float.parseFloat(s);
    } 
    catch (NumberFormatException nfe) {
    }
    if (f > 0) return f;

    return s.toUpperCase().equals("TRUE") ? 1 : 0;
  }

  // cast to PImage
  public PImage toPImage() {
    if (this.value == null) return new PImage();

    String s = this.value.toString();
    return ImageHelper.createImageFromBytes(ImageHelper.decodeBase64(s));
  }

  /*
  ** casting helper functions
   */

  private <T> boolean isAssignable(Class<T> to) {
    return to.isAssignableFrom(this.value.getClass()) ? true : false;
  }

  private boolean isNumeric(String s) {
    try {
      Double.parseDouble(s);
      return true;
    }
    catch (NumberFormatException nfe) {
      return false;
    }
  }

}


package com.nativelibs4java.pthread;
import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
import org.bridj.ann.Ptr;
/**
 * <i>native declaration : /usr/include/time.h:88</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("pthread") 
public class tm extends StructObject {
	static {
		BridJ.register();
	}
	/** seconds after the minute [0-60] */
	@Field(0) 
	public int tm_sec() {
		return this.io.getIntField(this, 0);
	}
	/** seconds after the minute [0-60] */
	@Field(0) 
	public tm tm_sec(int tm_sec) {
		this.io.setIntField(this, 0, tm_sec);
		return this;
	}
	/** minutes after the hour [0-59] */
	@Field(1) 
	public int tm_min() {
		return this.io.getIntField(this, 1);
	}
	/** minutes after the hour [0-59] */
	@Field(1) 
	public tm tm_min(int tm_min) {
		this.io.setIntField(this, 1, tm_min);
		return this;
	}
	/** hours since midnight [0-23] */
	@Field(2) 
	public int tm_hour() {
		return this.io.getIntField(this, 2);
	}
	/** hours since midnight [0-23] */
	@Field(2) 
	public tm tm_hour(int tm_hour) {
		this.io.setIntField(this, 2, tm_hour);
		return this;
	}
	/** day of the month [1-31] */
	@Field(3) 
	public int tm_mday() {
		return this.io.getIntField(this, 3);
	}
	/** day of the month [1-31] */
	@Field(3) 
	public tm tm_mday(int tm_mday) {
		this.io.setIntField(this, 3, tm_mday);
		return this;
	}
	/** months since January [0-11] */
	@Field(4) 
	public int tm_mon() {
		return this.io.getIntField(this, 4);
	}
	/** months since January [0-11] */
	@Field(4) 
	public tm tm_mon(int tm_mon) {
		this.io.setIntField(this, 4, tm_mon);
		return this;
	}
	/** years since 1900 */
	@Field(5) 
	public int tm_year() {
		return this.io.getIntField(this, 5);
	}
	/** years since 1900 */
	@Field(5) 
	public tm tm_year(int tm_year) {
		this.io.setIntField(this, 5, tm_year);
		return this;
	}
	/** days since Sunday [0-6] */
	@Field(6) 
	public int tm_wday() {
		return this.io.getIntField(this, 6);
	}
	/** days since Sunday [0-6] */
	@Field(6) 
	public tm tm_wday(int tm_wday) {
		this.io.setIntField(this, 6, tm_wday);
		return this;
	}
	/** days since January 1 [0-365] */
	@Field(7) 
	public int tm_yday() {
		return this.io.getIntField(this, 7);
	}
	/** days since January 1 [0-365] */
	@Field(7) 
	public tm tm_yday(int tm_yday) {
		this.io.setIntField(this, 7, tm_yday);
		return this;
	}
	/** Daylight Savings Time flag */
	@Field(8) 
	public int tm_isdst() {
		return this.io.getIntField(this, 8);
	}
	/** Daylight Savings Time flag */
	@Field(8) 
	public tm tm_isdst(int tm_isdst) {
		this.io.setIntField(this, 8, tm_isdst);
		return this;
	}
	/** offset from CUT in seconds */
	@Ptr 
	@Field(9) 
	public long tm_gmtoff() {
		return this.io.getSizeTField(this, 9);
	}
	/** offset from CUT in seconds */
	@Ptr 
	@Field(9) 
	public tm tm_gmtoff(long tm_gmtoff) {
		this.io.setSizeTField(this, 9, tm_gmtoff);
		return this;
	}
	/**
	 * timezone abbreviation<br>
	 * C type : char*
	 */
	@Field(10) 
	public Pointer<Byte > tm_zone() {
		return this.io.getPointerField(this, 10);
	}
	/**
	 * timezone abbreviation<br>
	 * C type : char*
	 */
	@Field(10) 
	public tm tm_zone(Pointer<Byte > tm_zone) {
		this.io.setPointerField(this, 10, tm_zone);
		return this;
	}
	public tm() {
		super();
	}
	public tm(Pointer pointer) {
		super(pointer);
	}
}

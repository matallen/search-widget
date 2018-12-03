package com.redhat.sso.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExHelper{

	
	public static int getIndexOf(String input, String regex){
		Matcher m=Pattern.compile(regex).matcher(input);
		if (m.find()){
			return m.start();
		}
		return -1;
	}
	
	public static String extract(String input, String regex){
		return extract(input, regex, 1);
	}
	public static String extract(String input, String regex, int group){
		Matcher m=Pattern.compile(regex).matcher(input);
		if (m.find()){
			return m.group(group);
		}
		return null;
	}
}

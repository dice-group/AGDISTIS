/*
 * Copyright (C) 2016 diegomoussallem
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.aksw.agdistis.util;

import java.io.IOException;
import java.util.StringTokenizer;

/**
 *
 * @author diegomoussallem
 */
public class Word2num {

	public static final String[] DIGITS = { "one", "two", "three", "four", "five", "six", "seven", "eight", "nine" };
	public static final String[] TENS = { null, "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty",
			"ninety" };
	public static final String[] TEENS = { "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
			"seventeen", "eighteen", "nineteen" };
	public static final String[] MAGNITUDES = { "hundred", "thousand", "million", "point" };
	public static final String[] ZERO = { "zero", "oh" };

	public String replaceNumbers(String input) {
		String result = "";
		String[] decimal = input.split(MAGNITUDES[3]);
		String[] millions = decimal[0].split(MAGNITUDES[2]);

		for (int i = 0; i < millions.length; i++) {
			String[] thousands = millions[i].split(MAGNITUDES[1]);

			for (int j = 0; j < thousands.length; j++) {
				int[] triplet = { 0, 0, 0 };
				StringTokenizer set = new StringTokenizer(thousands[j]);

				if (set.countTokens() == 1) { // If there is only one token
												// given in triplet
					String uno = set.nextToken();
					triplet[0] = 0;
					for (int k = 0; k < DIGITS.length; k++) {
						if (uno.equals(DIGITS[k])) {
							triplet[1] = 0;
							triplet[2] = k + 1;
						}
						if (uno.equals(TENS[k])) {
							triplet[1] = k + 1;
							triplet[2] = 0;
						}
					}
				} else if (set.countTokens() == 2) { // If there are two tokens
														// given in triplet
					String uno = set.nextToken();
					String dos = set.nextToken();
					if (dos.equals(MAGNITUDES[0])) { // If one of the two tokens
														// is "hundred"
						for (int k = 0; k < DIGITS.length; k++) {
							if (uno.equals(DIGITS[k])) {
								triplet[0] = k + 1;
								triplet[1] = 0;
								triplet[2] = 0;
							}
						}
					} else {
						triplet[0] = 0;
						for (int k = 0; k < DIGITS.length; k++) {
							if (uno.equals(TENS[k])) {
								triplet[1] = k + 1;
							}
							if (dos.equals(DIGITS[k])) {
								triplet[2] = k + 1;
							}
						}
					}
				} else if (set.countTokens() == 3) { // If there are three
														// tokens given in
														// triplet
					String uno = set.nextToken();
					String tres = set.nextToken();
					for (int k = 0; k < DIGITS.length; k++) {
						if (uno.equals(DIGITS[k])) {
							triplet[0] = k + 1;
						}
						if (tres.equals(DIGITS[k])) {
							triplet[1] = 0;
							triplet[2] = k + 1;
						}
						if (tres.equals(TENS[k])) {
							triplet[1] = k + 1;
							triplet[2] = 0;
						}
					}
				} else if (set.countTokens() == 4) { // If there are four tokens
														// given in triplet
					String uno = set.nextToken();
					String tres = set.nextToken();
					String cuatro = set.nextToken();
					for (int k = 0; k < DIGITS.length; k++) {
						if (uno.equals(DIGITS[k])) {
							triplet[0] = k + 1;
						}
						if (cuatro.equals(DIGITS[k])) {
							triplet[2] = k + 1;
						}
						if (tres.equals(TENS[k])) {
							triplet[1] = k + 1;
						}
					}
				} else {
					triplet[0] = 0;
					triplet[1] = 0;
					triplet[2] = 0;
				}

				result = result + Integer.toString(triplet[0]) + Integer.toString(triplet[1])
						+ Integer.toString(triplet[2]);
			}
		}

		if (decimal.length > 1) { // The number is a decimal
			StringTokenizer decimalDigits = new StringTokenizer(decimal[1]);
			result = result + ".";
			System.out.println(decimalDigits.countTokens() + " decimal digits");
			while (decimalDigits.hasMoreTokens()) {
				String w = decimalDigits.nextToken();
				System.out.println(w);

				if (w.equals(ZERO[0]) || w.equals(ZERO[1])) {
					result = result + "0";
				}
				for (int j = 0; j < DIGITS.length; j++) {
					if (w.equals(DIGITS[j])) {
						result = result + Integer.toString(j + 1);
					}
				}

			}
		}

		return result;
	}

	public static void main(String[] args) throws IOException {

		String word = "two thousand";
		Word2num w2n = new Word2num();
		String result;
		result = w2n.replaceNumbers(word);
		if (result.equals("000")) {
			System.out.println("not number");
		} else {
			System.out.println("number");
		}
		System.out.println(result);
	}
}

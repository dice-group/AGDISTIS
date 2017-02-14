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

/**
 *
 * @author diegomoussallem
 */
public class PreprocessingNLP {

	public String Preprocessing(String label) {

		Word2num w2n = new Word2num(); // test conversion of numbers
		// Label treatment
		// System.out.println("before preprocessing: " + label);
		String result = w2n.replaceNumbers(label);
		if (!result.equals("000")) {
			label = result;
		}
		label = label.trim();

		if (label.contains("#")) {
			label = label.replace("#", "");
		}
		if (label.contains(",")) {
			label = label.replace(",", "");
		}
		if (label.contains("_")) {
			label = label.replace("_", " ");
		}
		if (label.contains("-")) {
			label = label.replace("-", " ");
		}
		if (label.contains(".")) {
			label = label.replace(".", "");
		}

		if (label.equals(label.toUpperCase()) && label.length() > 4) {
			label = label.substring(0, 1).toUpperCase() + label.substring(1).toLowerCase();

		}

		if (label.equals(label.toLowerCase()) && label.length() > 4) {
			label = label.substring(0, 1).toUpperCase() + label.substring(1);
		}

		if (!label.contains(" ")) {
			label = label.replaceAll("(?<=[a-z])([A-Z])", " $1");
			label = label.trim();
		}
		// System.out.println("after preprocessing: " + label);
		return label;
	}
}

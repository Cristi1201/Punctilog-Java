package application;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.pipeline.CoNLLUReader;

public class PunctilogSentence {

	private ArrayList<ArrayList<String>> sentencePunctilog = new ArrayList<ArrayList<String>>();
	private String sentence = "";

	public static String initialProcessing(String text) {

		text = text.strip();

		if (text.charAt(text.length() - 1) == '.') {
			text = text.substring(0, text.length() - 1);
		}

		// excluderea conjunctiilor
		String[] conjunctii = { " dar ", " însă ", " căci ", " pentru că ", " deoarece ", " fiindcă ", " deși ", " ci ",
				" așa că ", " deci ", " prin urmare ", " așadar ", ", și ", " iar ", " şi apoi " };

		for (String conjunctie : conjunctii) {
			text = text.replaceAll(conjunctie, ", ");
			text = text.replaceAll(",, ", ", ");
			text = text.replaceAll(", ,", ", ");
		}

		// inlocuirea semnelor de punctuatie
		text = text.replaceAll(" \\?", "");
		text = text.replaceAll("\\? ", "");
		text = text.replaceAll(" \\!", "");
		text = text.replaceAll("\\! ", "");
		text = text.replaceAll("'", "\"");
		text = text.replaceAll("„", "\"");
		text = text.replaceAll("”", "\"");
		text = text.replaceAll(":", ",");

		// inlocuirea abrevierilor
		try {
			File abrevieri = new File("Abrevieri.txt");
			Scanner myReader = new Scanner(abrevieri);

			while (myReader.hasNextLine()) {
				String line = myReader.nextLine();
				line = line.strip();
				if (text.contains(line)) {
					text = text.replaceAll(line, String.valueOf(line.subSequence(0, line.length() - 1)));
				}
			}
			myReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return text;
	}

	public PunctilogSentence(CoNLLUReader.CoNLLUSentence conllSent) {

		// memorarea informatiilor despre cuvintele propozitiei
		try {
			for (String tokenLine : conllSent.tokenLines) {

				ArrayList<String> word = new ArrayList<String>();
				String[] splits = tokenLine.split("\t");

				word.add(splits[0]); // index CoNLLUReader.CoNLLU_IndexField
				word.add(splits[1]); // text CoNLLUReader.CoNLLU_WordField
				word.add(splits[2]); // lemma CoNLLUReader.CoNLLU_LemmaField
				word.add(splits[3]); // upos CoNLLUReader.CoNLLU_UPOSField
				word.add(splits[4]); // xpos CoNLLUReader.CoNLLU_XPOSField
				word.add(splits[5]); // feats CoNLLUReader.classShorthandToFull
				word.add(splits[6]); // depend (head) CoNLLUReader.CoNLLU_GovField
				word.add(splits[7]); // deprel CoNLLUReader.CoNLLU_RelnField
				word.add(splits[8]); // deps
				word.add(splits[9]); // start-end CoNLLUReader.CoNLLU_MiscField

				sentencePunctilog.add(word);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// unirea cuvintelor despartite de cratima
		// si memorarea propozitiei ca string
		for (int i = 0; i < sentencePunctilog.size(); i++) {
			String temp = sentencePunctilog.get(i).get(1);

			if (temp.substring(temp.length() - 1).equals("-")) {
				sentence = sentence + sentencePunctilog.get(i + 1).get(1) + " ";
				sentencePunctilog.get(i).set(1, sentencePunctilog.get(i).get(1) + sentencePunctilog.get(i + 1).get(1));
				sentencePunctilog.remove(i + 1);
			} else if (temp.substring(0, 1).equals("-")) {
				sentencePunctilog.get(i - 1).set(1,
						sentencePunctilog.get(i - 1).get(1) + sentencePunctilog.get(i).get(1));
				sentencePunctilog.remove(i);
			} else {
				sentence = sentence + sentencePunctilog.get(i).get(1) + " ";
			}
			sentence = sentence.replace(" , ", ", ");
		}
		sentence = sentence.substring(0, sentence.length() - 1);

		// setarea head pentru verb root id-ul sau
		// DONE -> ia index la root
		for (int i = 0; i < sentencePunctilog.size() - 1; i++) {
			if (Integer.parseInt((String) sentencePunctilog.get(i).get(6)) == 0) {
				sentencePunctilog.get(i).set(6, sentencePunctilog.get(i).get(0));
			}
		}

	}

	// VERIFICAREA EXPRESIILOR IN SENTENCE SI MARCAREA ACESTORA < >
	public ArrayList<ArrayList<String>> isExpression() {

		while (true) {
			ArrayList<Integer> indexExpression = new ArrayList<Integer>();
			try {
				File expresii = new File("all.txt");
				Scanner myReader = new Scanner(expresii);

				while (myReader.hasNextLine()) {
					String lines = myReader.nextLine();
					lines = lines.strip();
					if (this.getLemma().contains(lines)) {
						String[] line = lines.split(" ");
						String[] lemma = this.getLemma().split(" ");
						String[] words = this.getSentence().split(" ");

						try {
							for (int i = 0; i < line.length; i++) {
								for (int j = 0; j < lemma.length; j++) {
									if (line[i].equals(lemma[j]) && line[i + 1].equals(lemma[j + 1])) {
										while (line[i].equals(lemma[j])
												&& line[i + 1].equals(lemma[j + 1])) {
											indexExpression.add(j);
											i++;
											j++;
										}
									}
								}
							}
							
						} catch (Exception e) {
						}
						String temp = "";

						for (int i = 0; i < words.length; i++) {
							if (indexExpression.contains(i)) {
								temp += words[i] + "±";
							} else {
								temp += words[i] + " ";
							}
						}
						// eliminarea ultimului spatiu
						this.sentence = temp.substring(0, temp.length() - 1);
						break;
					}
				}
				myReader.close();
			} catch (Exception e) {
			}

			// inserarea < > in sentence
			if (indexExpression.size() != 0) {
				indexExpression.add(indexExpression.get(indexExpression.size() - 1) + 1);
				String index = "", text = "<", lemma = "", upos = "", xpos = "", feats = "", deprel = "", deps = "",
						start_end = "";
				double dependency = 0;
				int dependencyCount = 0;

				for (int i : indexExpression) {
					text += " " + sentencePunctilog.get(i).get(1);
					dependency += Double.parseDouble(sentencePunctilog.get(i).get(6));
					dependencyCount++;
				}
				text += ">";
				text = text.replace("< ", "<");
				int j = -1;

				// se memoreaza descrierea campurilor pt a se insera ca descriere la expresie
				for (int i : indexExpression) {
					if (sentencePunctilog.get(i).get(3).equals("VERB")
							|| sentencePunctilog.get(i).get(3).equals("AUX")) {
						index = sentencePunctilog.get(i).get(0);
						lemma += sentencePunctilog.get(i).get(2) + " ";
						upos = sentencePunctilog.get(i).get(3);
						xpos = sentencePunctilog.get(i).get(4);
						feats = sentencePunctilog.get(i).get(5);
						deprel = sentencePunctilog.get(i).get(7);
						deps = sentencePunctilog.get(i).get(8);
						start_end = sentencePunctilog.get(i).get(9);
						j = i;
						break;
					} else if (sentencePunctilog.get(i).get(3).equals("NOUN")
							|| sentencePunctilog.get(i).get(3).equals("ADJ")) {
						index = sentencePunctilog.get(i).get(0);
						lemma += sentencePunctilog.get(i).get(2) + " ";
						upos = sentencePunctilog.get(i).get(3);
						xpos = sentencePunctilog.get(i).get(4);
						feats = sentencePunctilog.get(i).get(5);
						deprel = sentencePunctilog.get(i).get(7);
						deps = sentencePunctilog.get(i).get(8);
						start_end = sentencePunctilog.get(i).get(9);
					}
					j = i;
				}
				if (upos.equals("")) {
					index = (String) sentencePunctilog.get(j).get(0);
					lemma += (String) sentencePunctilog.get(j).get(2) + " ";
					upos = (String) sentencePunctilog.get(j).get(3);
					xpos = (String) sentencePunctilog.get(j).get(4);
					feats = (String) sentencePunctilog.get(j).get(5);
					deprel = (String) sentencePunctilog.get(j).get(7);
					deps = (String) sentencePunctilog.get(j).get(8);
					start_end = (String) sentencePunctilog.get(j).get(9);
				}

				j = indexExpression.get(0);
				sentencePunctilog.get(j).set(0, index);
				sentencePunctilog.get(j).set(1, text);
				sentencePunctilog.get(j).set(2, lemma.substring(0, lemma.length() - 1));
				sentencePunctilog.get(j).set(3, upos);
				sentencePunctilog.get(j).set(4, xpos);
				sentencePunctilog.get(j).set(5, feats);
				sentencePunctilog.get(j).set(6, String.valueOf(dependency / dependencyCount));
				sentencePunctilog.get(j).set(7, deprel);
				sentencePunctilog.get(j).set(8, deps);
				sentencePunctilog.get(j).set(9, start_end);
				
				while (indexExpression.size() > 1) {
					int delete = indexExpression.get(indexExpression.size()-1);
					sentencePunctilog.remove(delete);
					indexExpression.remove(indexExpression.size()-1);
				}
					
//				for (int k : indexExpression.subList(1, indexExpression.size())) {
//					// revenirea la noul index al elementului care trebuie sters
//					k -= countDeleted;
//					sentencePunctilog.remove(k);
//					countDeleted++;
//				}

			} else {
				break;
			}
		}
		
		return sentencePunctilog;
	}

	public ArrayList<ArrayList<String>> isDirectSpeech() {

		int nrQuotationMark = 0;

		for (int indexLine = 0; indexLine < sentencePunctilog.size(); indexLine++) {
			if (sentencePunctilog.get(indexLine).get(1).equals("\"")) {
				nrQuotationMark++;
				if (nrQuotationMark != 0 && nrQuotationMark % 2 == 0) {

					boolean openBrackets = false, openBracketsOnce = false;

					ArrayList<ArrayList<String>> list = new ArrayList<ArrayList<String>>();

					for (int i = 0; i < sentencePunctilog.size(); i++) {
						if (sentencePunctilog.get(i).get(1).equals("\"") && !openBrackets && !openBracketsOnce) {
							sentencePunctilog.get(i + 1).set(1, "[" + sentencePunctilog.get(i + 1).get(1));
							openBrackets = true;
							openBracketsOnce = true;
						} else if (sentencePunctilog.get(i).get(1).equals("\"") && openBrackets) {
							sentencePunctilog.get(i - 1).set(1, sentencePunctilog.get(i - 1).get(1) + "]");
							openBrackets = false;
						}
					}

					sentencePunctilog.forEach(s -> {
						if (!s.get(1).equals("\"")) {
							list.add(s);
						}
					});

					ArrayList<String> pos = new ArrayList<String>();
					ArrayList<Integer> indexToDelete = new ArrayList<Integer>();
					ArrayList<ArrayList<Integer>> listToDelete = new ArrayList<ArrayList<Integer>>();
					String directSpeech = "";

					for (int i = 0; i < list.size(); i++) {
						if ((list.get(i).get(1)).substring(0, 1).equals("[")) {

							for (int j = i; j < list.size(); j++) {

								indexToDelete.add(j);

								pos.add(list.get(j).get(0));
								directSpeech += " " + list.get(j).get(1);
								pos.add(list.get(j).get(2));
								pos.add(list.get(j).get(3));
								pos.add(list.get(j).get(4));
								pos.add(list.get(j).get(5));
								pos.add(list.get(j).get(6));
								pos.add(list.get(j).get(7));
								pos.add(list.get(j).get(8));
								pos.add(list.get(j).get(9));

								if (list.get(j).get(1).charAt(list.get(j).get(1).length() - 1) == ']') {
									indexToDelete.remove(0);
									listToDelete.add(indexToDelete);

									list.get(i).set(1, directSpeech.substring(1));

									// se parcurge doar pe campurile UPOS, restul campurilor se extrag dupa
									// indexuri
									for (int k = 0; k < pos.size(); k += 9) {
										// verificare camp UPOS
										if (pos.get(k + 2).equals("VERB")) {
											list.get(i).set(0, pos.get(k));
											list.get(i).set(2, pos.get(k + 1));
											list.get(i).set(3, "VERB");
											list.get(i).set(4, pos.get(k + 3));
											list.get(i).set(5, pos.get(k + 4));
											list.get(i).set(6, pos.get(k + 5));
											list.get(i).set(7, pos.get(k + 6));
											list.get(i).set(8, pos.get(k + 7));
											list.get(i).set(9, pos.get(k + 8));
											break;
										} else if (pos.get(k + 2).equals("NOUN")) {
											list.get(i).set(0, pos.get(k));
											list.get(i).set(2, pos.get(k + 1));
											list.get(i).set(3, "NOUN");
											list.get(i).set(4, pos.get(k + 3));
											list.get(i).set(5, pos.get(k + 4));
											list.get(i).set(6, pos.get(k + 5));
											list.get(i).set(7, pos.get(k + 6));
											list.get(i).set(8, pos.get(k + 7));
											list.get(i).set(9, pos.get(k + 8));
										}
									}
									directSpeech = "";
									pos = new ArrayList<String>();
									indexToDelete = new ArrayList<Integer>();
									break;
								}
							}
						}
					}

					if (!listToDelete.isEmpty() && listToDelete.get(0).size() > 0) {

						for (int i = listToDelete.get(0).get(listToDelete.get(0).size() - 1); listToDelete.get(0)
								.size() != 0; i--) {
							list.remove(i);
							listToDelete.get(0).remove((Integer) i);
						}
					}
					sentence = "";
					sentencePunctilog.clear();
					list.forEach(line -> {
						sentencePunctilog.add(line);
						sentence = sentence + line.get(1) + " ";
					});

					sentence = sentence.substring(0, sentence.length() - 1);

					return list;
				}
			}
		}

		return sentencePunctilog;
	}

	public ArrayList<ArrayList<String>> NER() {

		Pattern pattern = Pattern.compile(
				"(([A-Z]+[\\w+]* ){1,}([A-Z]+[\\w+]*) [\\w+]{1,3} ([A-Z]+[\\w+]*))|(([A-Z]+[\\w+]* ){1,}([A-Z]+[\\w+]*))");

		Matcher matcher = pattern.matcher(sentence);
		try {
			while (matcher.find()) {

				int indexStart = -1, indexEnd = -1;

				for (int i = 0; i < sentencePunctilog.size(); i++) {
					if (sentencePunctilog.get(i).get(1).equals(matcher.group().split(" ")[0])) {
						indexStart = i;
					}
					if (sentencePunctilog.get(i).get(1)
							.equals(matcher.group().split(" ")[matcher.group().split(" ").length - 1])) {
						indexEnd = i + 1;
					}
				}

				String text = "";
				for (int i = indexStart; i < indexEnd; i++) {
					text += sentencePunctilog.get(i).get(1) + " ";
				}
				text = "<" + text.substring(0, text.length() - 1) + ">";

				sentencePunctilog.get(indexStart).set(1, text);

				if (sentencePunctilog.get(indexStart).get(3).equals("ADP")
						|| sentencePunctilog.get(indexStart).get(3).equals("DET")) {
					sentencePunctilog.get(indexStart).set(3, "NOUN");
				}
				for (int i = indexEnd - 1; i > indexStart; i--) {
					sentencePunctilog.remove(i);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sentencePunctilog;
	}

	public ArrayList<ArrayList<String>> isModifier(ArrayList<ArrayList<String>> subSentence) {

		for (int i = 0; i < subSentence.size() - 1; i++) {

			if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
//			} else if (subSentence.get(i+1).get(3).equals("VERB")) {
//				subSentence.get(i+1).set(1, ":" + subSentence.get(i+1).get(1));
			} else if (subSentence.get(i + 1).get(3).equals("AUX")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("obl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nmod") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("cc") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
//			} else if (subSentence.get(i).get(7).equals("aux") && subSentence.get(i+1).get(7).toLowerCase().equals("root")) {
//				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
//			}
			} else if (subSentence.get(i).get(7).equals("obl") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("obl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obl") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("nmod:pmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nmod") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("obj") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("nsubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("conj") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("cop")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1));
			} else if (subSentence.get(i).get(7).equals("aux:pass")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advcl") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nsubj")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nummod") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl:pv")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("punct")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("nmod:agent")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advmod")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("advmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl:pass")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obj") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("aux") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("nummod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("acl") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nmod") && subSentence.get(i + 1).get(7).equals("nummod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("ccomp") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("conj") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("aux") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("aux") && subSentence.get(i + 1).get(7).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obl") && subSentence.get(i + 1).get(7).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("amod") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("nsubj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("expl:pv") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obl") && subSentence.get(i + 1).get(7).equals("nummod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nsubj:pass") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nmod:pmod") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("conj") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(7).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nummod") && subSentence.get(i + 1).get(7).equals("case")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nummod") && subSentence.get(i + 1).get(7).equals("obl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("amod") && subSentence.get(i + 1).get(7).equals("nsubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nsubj:pass") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("expl:pv") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("iobj") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("iobj") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("nmod:pmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obj")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("nummod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("nmod:tmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nmod") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nmod:pmod") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("amod") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nmod") && subSentence.get(i + 1).get(7).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("aux:pass") && subSentence.get(i + 1).get(7).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("amod") && subSentence.get(i + 1).get(7).equals("obl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("det")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("appos") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("ccomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("conj") && subSentence.get(i + 1).get(7).equals("flat")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("expl:pv") && subSentence.get(i + 1).get(7).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("conj") && subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("iobj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("xcomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nmod:agent") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("ccomp") && subSentence.get(i + 1).get(7).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("conj") && subSentence.get(i + 1).get(7).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("advcl") && subSentence.get(i + 1).get(1).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("obl") && subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("nsubj:pass")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("acl") && subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nummod") && subSentence.get(i + 1).get(7).equals("nsubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obj") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("aux") && subSentence.get(i + 1).get(7).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("parataxis") && subSentence.get(i + 1).get(7).equals("nsubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl:pv") && subSentence.get(i + 1).get(7).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nmod") && subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("obj") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("case")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("acl") && subSentence.get(i + 1).get(7).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("amod") && subSentence.get(i + 1).get(7).equals("iobj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("obl") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("nsubj:pass")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("advmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("cop") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advcl") && subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("det")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nummod") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nmod:tmod")
					&& subSentence.get(i + 1).get(7).equals("nummod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("iobj")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("iobj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obj") && subSentence.get(i + 1).get(7).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("amod") && subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("nummod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("acl") && subSentence.get(i + 1).get(7).equals("iobj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("cop") && subSentence.get(i + 1).get(7).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("aux:pass") && subSentence.get(i + 1).get(7).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("cop") && subSentence.get(i + 1).get(7).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("nmod:agent")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("ccomp:pmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("amod") && subSentence.get(i + 1).get(7).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("parataxis") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("appos")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("xcomp") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("appos") && subSentence.get(i + 1).get(1).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl:pass") && subSentence.get(i + 1).get(7).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nummod") && subSentence.get(i + 1).get(7).equals("goeswith")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nmod:agent") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("cop") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("goeswith")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("obl")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("xcomp") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("iobj") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("nsubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("conj") && subSentence.get(i + 1).get(7).equals("xcomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("aux:pass") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl:poss")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obl") && subSentence.get(i + 1).get(7).equals("flat")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("ccomp:pmod") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("obl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("conj") && subSentence.get(i + 1).get(7).equals("nsubj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("acl") && subSentence.get(i + 1).get(7).equals("nsubj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("advcl") && subSentence.get(i + 1).get(7).equals("expl:pv")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("iobj") && subSentence.get(i + 1).get(7).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obj") && subSentence.get(i + 1).get(7).equals("advcl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advcl") && subSentence.get(i + 1).get(7).equals("iobj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("case")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("csubj") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("cc") && subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("ccomp") && subSentence.get(i + 1).get(7).equals("nsubj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("expl:pass") && subSentence.get(i + 1).get(7).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nummod") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("acl") && subSentence.get(i + 1).get(7).equals("ccomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("nmod:tmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obj") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("aux") && subSentence.get(i + 1).get(7).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("aux:pass") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("aux") && subSentence.get(i + 1).get(7).equals("advcl:tcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("iobj") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("acl") && subSentence.get(i + 1).get(7).equals("xcomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("expl:poss") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl:poss") && subSentence.get(i + 1).get(7).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl:poss") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("conj") && subSentence.get(i + 1).get(7).equals("iobj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nsubj:pass") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("expl") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl:pass") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(7).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(7).equals("parataxis")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nummod:tmod")
					&& subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nmod") && subSentence.get(i + 1).get(7).equals("punct")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nummod") && subSentence.get(i + 1).get(7).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nummod") && subSentence.get(i + 1).get(7).equals("punct")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nmod:tmod") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("advcl:tcl") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("obj") && subSentence.get(i + 1).get(7).equals("nummod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("obl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("advmod")
					&& subSentence.get(i + 1).get(7).equals("parataxis")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl") && subSentence.get(i + 1).get(7).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("aux:pass") && subSentence.get(i + 1).get(7).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advcl") && subSentence.get(i + 1).get(7).equals("nsubj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("expl:impres")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("xcomp") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nmod:agent") && subSentence.get(i + 1).get(7).equals("flat")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nmod") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("conj") && subSentence.get(i + 1).get(7).equals("ccomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("expl")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("parataxis") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("mark")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl") && subSentence.get(i + 1).get(7).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advcl") && subSentence.get(i + 1).get(7).equals("ccomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("aux") && subSentence.get(i + 1).get(7).equals("parataxis")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("appos") && subSentence.get(i + 1).get(7).equals("punct")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nummod")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("expl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("ccomp") && subSentence.get(i + 1).get(7).equals("xcomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nmod") && subSentence.get(i + 1).get(7).equals("case")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("expl:pv") && subSentence.get(i + 1).get(7).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("iobj") && subSentence.get(i + 1).get(7).equals("parataxis")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advcl") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("amod") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advmod")
					&& subSentence.get(i + 1).get(7).equals("nsubj:pass")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("aux") && subSentence.get(i + 1).get(7).equals("csubj:pass")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl:poss") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nummod")
					&& subSentence.get(i + 1).get(7).equals("nsubj:pass")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("aux:pass")
					&& subSentence.get(i + 1).get(7).equals("parataxis")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("iobj") && subSentence.get(i + 1).get(7).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("iobj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("appos")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obl") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("csubj") && subSentence.get(i + 1).get(7).equals("iobj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("expl:poss") && subSentence.get(i + 1).get(7).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obl") && subSentence.get(i + 1).get(7).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("obl") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("ccomp") && subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("advcl") && subSentence.get(i + 1).get(7).equals("xcomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nmod") && subSentence.get(i + 1).get(7).equals("nsubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nmod") && subSentence.get(i + 1).get(7).equals("goeswith")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nmod:pmod") && subSentence.get(i + 1).get(7).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("obj") && subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nummod") && subSentence.get(i + 1).get(7).equals("nummod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("case")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("obl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("advcl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("cc:personaj")
					&& subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("appos") && subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("nummod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("ccomp") && subSentence.get(i + 1).get(7).equals("obl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("iobj") && subSentence.get(i + 1).get(7).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl") && subSentence.get(i + 1).get(7).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nummod") && subSentence.get(i + 1).get(7).equals("compound")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("iobj") && subSentence.get(i + 1).get(7).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl:pv")
					&& subSentence.get(i + 1).get(7).equals("parataxis")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("cop")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nmod:pmod")
					&& subSentence.get(i + 1).get(7).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("iobj") && subSentence.get(i + 1).get(7).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("csubj") && subSentence.get(i + 1).get(7).equals("nsubj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("cop") && subSentence.get(i + 1).get(7).equals("parataxis")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("appos") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("parataxis")
					&& subSentence.get(i + 1).get(7).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("obj") && subSentence.get(i + 1).get(7).equals("#")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("ccomp") && subSentence.get(i + 1).get(7).equals("iobj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("parataxis")
					&& subSentence.get(i).get(7).equals("nsubj:pass")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nummod")
					&& subSentence.get(i + 1).get(7).equals("nummod:pmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obl") && subSentence.get(i + 1).get(7).equals("punct")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("csubj")
					&& subSentence.get(i + 1).get(7).equals("nsubj:pass")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("csubj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("advcl")
					&& subSentence.get(i + 1).get(7).equals("nsubj:pass")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("advcl") && subSentence.get(i + 1).get(7).equals("obl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("aux") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl:impress")
					&& subSentence.get(i + 1).get(7).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("det")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("aux:pass")
					&& subSentence.get(i + 1).get(7).equals("advcl:tcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("amod") && subSentence.get(i + 1).get(7).equals("iobj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("iobj") && subSentence.get(i + 1).get(7).equals("flat")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("conj") && subSentence.get(i + 1).get(7).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("parataxis") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("acl") && subSentence.get(i + 1).get(7).equals("nsubj:pass")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("parataxis") && subSentence.get(i + 1).get(7).equals("expl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("amod") && subSentence.get(i + 1).get(7).equals("nsubj:pass")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nsubj:pass") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(7).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obl") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("amod") && subSentence.get(i + 1).get(7).equals("nmod:agent")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obj") && subSentence.get(i + 1).get(7).equals("ccomp:pmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("appos") && subSentence.get(i + 1).get(7).equals("nummod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("parataxis")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nsubj:pass") && subSentence.get(i + 1).get(7).equals("flat")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("acl") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(7).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("conj") && subSentence.get(i + 1).get(7).equals("advcl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nmod:agent") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("conj") && subSentence.get(i + 1).get(7).equals("nummod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("cop") && subSentence.get(i + 1).get(7).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nummod") && subSentence.get(i + 1).get(7).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("appos") && subSentence.get(i + 1).get(7).equals("goeswith")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(7).equals("advcl:tcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("goeswith")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("parataxis") && subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("aux") && subSentence.get(i + 1).get(7).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obj") && subSentence.get(i + 1).get(7).equals("parataxis")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("parataxis") && subSentence.get(i + 1).get(7).equals("xcomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("fixed") && subSentence.get(i + 1).get(7).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nmod:pmod") && subSentence.get(i + 1).get(7).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl:pv")
					&& subSentence.get(i + 1).get(7).equals("advcl:tcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("xcomp") && subSentence.get(i + 1).get(7).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("conj") && subSentence.get(i + 1).get(7).equals("expl:poss")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("expl:poss")
					&& subSentence.get(i + 1).get(7).equals("orphan")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obj") && subSentence.get(i + 1).get(7).equals("advcl:tcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("amod") && subSentence.get(i + 1).get(7).equals("nmod:pmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nmod") && subSentence.get(i + 1).get(7).equals("obl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("cc")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("flat") && subSentence.get(i + 1).get(7).equals("nummod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("amod") && subSentence.get(i + 1).get(7).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("aux:pass") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("aux") && subSentence.get(i + 1).get(7).equals("appos")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("appos") && subSentence.get(i + 1).get(7).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("flat") && subSentence.get(i + 1).get(7).equals("flat")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nummod") && subSentence.get(i + 1).get(7).equals("appos")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nmod:pmod") && subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("amod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("expl:impres")
					&& subSentence.get(i + 1).get(7).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nmod:agent") && subSentence.get(i + 1).get(7).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("parataxis") && subSentence.get(i + 1).get(7).equals("iobj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("cc:preconj") && subSentence.get(i + 1).get(7).equals("obj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nsubj:pass")
					&& subSentence.get(i + 1).get(7).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("acl") && subSentence.get(i + 1).get(7).equals("csubj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("expl:pass") && subSentence.get(i + 1).get(7).equals("obl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obl:pmod") && subSentence.get(i + 1).get(7).equals("nmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("ADJ")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("amod")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("det") && subSentence.get(i + 1).get(7).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("aux")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("mark")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("advcl") && subSentence.get(i + 1).get(7).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("aux") && subSentence.get(i + 1).get(7).equals("aux")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("iobj") && subSentence.get(i + 1).get(7).equals("aux")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("obj") && subSentence.get(i + 1).get(7).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("advmod")
					&& subSentence.get(i + 1).get(7).equals("expl:pass")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nsubj:pass") && subSentence.get(i + 1).get(7).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("cop") && subSentence.get(i + 1).get(7).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
					&& subSentence.get(i + 1).get(7).equals("case")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(7).equals("case")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i + 1).get(7).equals("mark")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("nmod")
					&& subSentence.get(i + 1).get(7).toLowerCase().equals("root")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(7).equals("csubj") && subSentence.get(i + 1).get(7).equals("xcomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			}
		}

		return subSentence;
	}

	public ArrayList<ArrayList<String>> isCalificator(ArrayList<ArrayList<String>> subSentence) {

		ArrayList<Integer> toDelete = new ArrayList<Integer>();
		int i = 0;

		while (i < subSentence.size()) {
			
			try {
				if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("ADV")
						&& subSentence.get(i + 2).get(3).equals("ADV")) {
					subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " ("
							+ subSentence.get(i + 1).get(1) + " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("DET")
						&& subSentence.get(i + 2).get(3).equals("ADJ") && subSentence.get(i+2).get(7).equals("amod")) {
					subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " ("
							+ subSentence.get(i + 1).get(1) + " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if (subSentence.get(i).get(3).equals("ADJ") && subSentence.get(i + 1).get(3).equals("ADJ")
						&& subSentence.get(i + 2).get(3).equals("ADJ")) {
					subSentence.get(i + 2).set(1, "<" + subSentence.get(i).get(1) + " "
							+ subSentence.get(i + 1).get(1) + " " + subSentence.get(i + 2).get(1) + ">");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADV")
						&& subSentence.get(i + 2).get(3).equals("ADV")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " ("
							+ subSentence.get(i + 1).get(1) + " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i + 1);
					toDelete.add(i + 2);
					i += 2;
				} else if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("ADV")
						&& subSentence.get(i + 2).get(3).equals("ADJ")) {
					subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " ("
							+ subSentence.get(i + 1).get(1) + " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if ((subSentence.get(i).get(1).toLowerCase().contains("cel")
						|| subSentence.get(i).get(1).toLowerCase().contains("cea")
						|| subSentence.get(i).get(1).toLowerCase().contains("cei"))
						&& subSentence.get(i + 1).get(1).contains("mai")
						&& (subSentence.get(i + 2).get(3).equals("ADJ") || subSentence.get(i + 2).get(3).equals("ADV") || subSentence.get(i+2).get(3).equals("NOUN"))) {
					subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1)
							+ " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(7).equals("nmod")
						&& subSentence.get(i + 2).get(7).equals("nmod")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " ("
							+ subSentence.get(i + 1).get(1) + " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i + 1);
					toDelete.add(i + 2);
					i += 2;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(7).equals("amod")
						&& subSentence.get(i + 2).get(7).equals("nmod")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " ("
							+ subSentence.get(i + 1).get(1) + " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i + 1);
					toDelete.add(i + 2);
					i += 2;
				} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("NUM")
						&& subSentence.get(i + 2).get(3).equals("NUM")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " ("
							+ subSentence.get(i + 1).get(1) + " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i + 1);
					toDelete.add(i + 2);
					i += 2;
				}
			} catch (Exception e) {
			}
			
			try {
				if ((subSentence.get(i).get(3).equals("ADV") || subSentence.get(i).get(3).equals("ADJ"))
						&& (subSentence.get(i + 1).get(3).equals("ADJ")
								|| subSentence.get(i + 1).get(3).equals("ADV"))) {
					subSentence.get(i + 1).set(1,
							"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
					toDelete.add(i);
					i += 1;
				} else if (subSentence.get(i).get(1).contains("din") && subSentence.get(i + 1).get(1).contains("nou")) {
					subSentence.get(i + 1).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("meu")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("mea") && subSentence.get(i+1).get(1).length() <= 4) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("mei")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				}  else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("mele")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("tău")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("ta") && subSentence.get(i + 1).get(1).length() <= 3) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("tăi")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("tale")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("său")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("sa") && subSentence.get(i + 1).get(1).length() <= 3) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("săi")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				}else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("sale")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("nostru")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("noastră")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("noștri")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("noastre")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("vostru")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("voastră")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("voștri")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("voastre")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("lor") && subSentence.get(i + 1).get(1).length() <= 5) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(1).contains("ei") && subSentence.get(i + 1).get(1).length() <= 3) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i + 1).get(1).length() <= 5 && subSentence.get(i + 1).get(1).contains("tine")) {
					subSentence.get(i).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(7).equals("nummod") && subSentence.get(i + 1).get(3).equals("NOUN")) {
					subSentence.get(i + 1).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i+1).get(1) + ")");
					toDelete.add(i);
					i += 1;
				}
			} catch (Exception e) {
			}
			
			i += 1;
		}

		while (toDelete.size() != 0) {
			int delete = toDelete.get(toDelete.size() - 1);
			subSentence.remove(delete);
			toDelete.remove(toDelete.size() - 1);
		}

		for (i = 0; i < subSentence.size(); i++) {
			if (subSentence.get(i).get(3).equals("PROPN")) {
				subSentence.get(i).set(3, "PRON");
			}
		}

		return subSentence;
	}

	public ArrayList<ArrayList<String>> isVerbPart(ArrayList<ArrayList<String>> subSentence) {

		ArrayList<Integer> toDelete = new ArrayList<Integer>();
		int i = 0;
		while (i < subSentence.size()) {
			ArrayList<ArrayList<String>> temp = new ArrayList<ArrayList<String>>();
			for (int j = 0; j < subSentence.size(); j++) {
				temp.add(new ArrayList<String>(subSentence.get(j)));
			}

			try {
				if (temp.equals(this.firstCheckIsVerbPart(subSentence, i))) {
					try {
						if (temp.equals(this.secondCheckIsVerbPart(subSentence, i))) {
							try {
								if (!temp.equals(this.thirdCheckIsVerbPart(subSentence, i))) {
									toDelete.add(i);
									i += 1;
								}
							} catch (Exception third) {
							}
						} else {
							toDelete.add(i);
							toDelete.add(i + 1);
							i += 2;
						}
					} catch (Exception second) {
						try {
							if (!temp.equals(this.thirdCheckIsVerbPart(subSentence, i))) {
								toDelete.add(i);
								i += 1;
							}
						} catch (Exception third) {
						}
					}

				} else {
					toDelete.add(i);
					toDelete.add(i + 1);
					toDelete.add(i + 2);
					i += 3;
				}

			} catch (Exception first) {
				try {
					if (temp.equals(this.secondCheckIsVerbPart(subSentence, i))) {
						try {
							if (!temp.equals(this.thirdCheckIsVerbPart(subSentence, i))) {
								toDelete.add(i);
								i += 1;
							}
						} catch (Exception third) {
						}
					} else {
						toDelete.add(i);
						toDelete.add(i + 1);
						i += 2;
					}
				} catch (Exception second) {
					try {
						if (!temp.equals(this.thirdCheckIsVerbPart(subSentence, i))) {
							toDelete.add(i);
							i += 1;
						}
					} catch (Exception third) {
					}
				}
			}
			i += 1;
		}

		while (toDelete.size() != 0) {
			int delete = toDelete.get(toDelete.size() - 1);
			subSentence.remove(delete);
			toDelete.remove(toDelete.size() - 1);
		}
		i = 0;
		while (i < subSentence.size()) {
			try {
				if (subSentence.get(i).get(1).toLowerCase().equals("nu")
						|| subSentence.get(i).get(1).toLowerCase().equals("nu:")
						|| subSentence.get(i).get(1).equals(":nu") || subSentence.get(i).get(1).equals(":nu:")) {
					subSentence.get(i + 1).set(1,
							"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
					toDelete.add(i);
					i += 1;
				} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(3).equals("VERB")) {
					subSentence.get(i + 1).set(1,
							"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
					toDelete.add(i);
					i += 1;
				} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(3).equals("VERB")) {
					subSentence.get(i + 1).set(1,
							"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
					toDelete.add(i);
					i += 1;
				}
			} catch (Exception e) {
			}
			i += 1;
		}

		while (toDelete.size() != 0) {
			int delete = toDelete.get(toDelete.size() - 1);
			subSentence.remove(delete);
			toDelete.remove(toDelete.size() - 1);
		}

		return subSentence;
	}

	private ArrayList<ArrayList<String>> firstCheckIsVerbPart(ArrayList<ArrayList<String>> subSentence, int i) {
		if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("expl:pv")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentence.get(i + 3).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " ("
					+ subSentence.get(i + 2).get(1) + " " + subSentence.get(i + 3).get(1) + ")))");
		} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("mark")
				&& subSentence.get(i + 2).get(3).equals("PRON") && subSentence.get(i + 3).get(3).equals("AUX")) {
			subSentence.get(i + 3).set(1, "(" + subSentence.get(i).get(1) + " ((" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + ") " + subSentence.get(i + 3).get(1) + "))");
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("PART")
				&& subSentence.get(i + 2).get(3).equals("ADV") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentence.get(i + 3).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " ("
					+ subSentence.get(i + 2).get(1) + " " + subSentence.get(i + 3).get(1) + ")))");
		} else if (subSentence.get(i).get(3).equals("PART") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentence.get(i + 3).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " ("
					+ subSentence.get(i + 2).get(1) + " " + subSentence.get(i + 3).get(1) + ")))");
		} else if (subSentence.get(i).get(3).equals("PART") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentence.get(i + 3).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " ("
					+ subSentence.get(i + 2).get(1) + " " + subSentence.get(i + 3).get(1) + ")))");
		} else if (subSentence.get(i).get(7).equals("iobj") && subSentence.get(i + 1).get(7).equals("expl:pv")
				&& subSentence.get(i + 2).get(7).equals("aux") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentence.get(i + 3).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " ("
					+ subSentence.get(i + 2).get(1) + " " + subSentence.get(i + 3).get(1) + ")))");
		} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("mark")
				&& subSentence.get(i + 2).get(7).equals("expl:poss") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentence.get(i + 3).set(1, "((" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + ")) " + subSentence.get(i + 3).get(1) + ")");
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(7).equals("expl:pass")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentence.get(i + 3).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " ("
					+ subSentence.get(i + 2).get(1) + " " + subSentence.get(i + 3).get(1) + ")))");
		} else if ((subSentence.get(i).get(1).equals("să") || subSentence.get(i).get(1).equals("a"))
				&& subSentence.get(i + 1).get(1).equals("i") && subSentence.get(i + 2).get(1).equals("se")
				&& subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentence.get(i + 3).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " ("
					+ subSentence.get(i + 2).get(1) + " " + subSentence.get(i + 3).get(1) + ")))");
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(1).equals("se")
				&& subSentence.get(i + 2).get(1).equals("mai") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentence.get(i + 3).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " ("
					+ subSentence.get(i + 2).get(1) + " " + subSentence.get(i + 3).get(1) + ")))");
		} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("mark")
				&& subSentence.get(i + 2).get(7).equals("expl:pv") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentence.get(i + 3).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " ("
					+ subSentence.get(i + 2).get(1) + " " + subSentence.get(i + 3).get(1) + ")))");
		}

		return subSentence;
	}

	private ArrayList<ArrayList<String>> secondCheckIsVerbPart(ArrayList<ArrayList<String>> subSentence, int i) {
		if ((subSentence.get(i).get(3).equals("AUX") || subSentence.get(i).get(7).equals("expl")
				|| subSentence.get(i).get(7).equals("iobj") || subSentence.get(i).get(7).equals("expl:poss"))
				&& subSentence.get(i + 1).get(3).equals("AUX") && subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, ":((" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ") "
					+ subSentence.get(i + 2).get(1) + ")");
		} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i).get(3).equals("PART")
				&& (subSentence.get(i + 1).get(7).equals("iobj") || subSentence.get(i + 1).get(7).equals("obj"))
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("obj")
				&& subSentence.get(i + 2).get(7).toLowerCase().equals("root")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if ((subSentence.get(i).get(7).equals("mark") || subSentence.get(i).get(7).equals("acl"))
				&& subSentence.get(i + 1).get(7).equals("mark") && subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if ((subSentence.get(i).get(7).equals("expl:pv") || subSentence.get(i).get(7).equals("obj")
				|| subSentence.get(i).get(7).equals("expl:pass")) && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if (subSentence.get(i).get(3).equals("PART") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if (subSentence.get(i).get(3).equals("PART") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("AUX")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if (subSentence.get(i).get(3).equals("AUX") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if ((subSentence.get(i).get(1).equals("să") || subSentence.get(i).get(1).equals("a"))
				&& subSentence.get(i + 1).get(1).equals("se") && subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if (subSentence.get(i).get(1).toLowerCase().equals("se")
				&& (subSentence.get(i + 1).get(1).equals("va") || subSentence.get(i + 1).get(1).equals("vor"))
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(1).equals("se")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("PART")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if ((subSentence.get(i).get(1).equals("se") || subSentence.get(i).get(1).equals("se:"))
				&& subSentence.get(i + 1).get(3).equals("VERB") && subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "((" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ") "
					+ subSentence.get(i + 2).get(1) + ")");
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(1).equals("se")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("PART")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if ((subSentence.get(i).get(1).toLowerCase().equals("se") || subSentence.get(i).get(1).equals("se:"))
				&& subSentence.get(i + 1).get(3).equals("VERB") && subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "((" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ") "
					+ subSentence.get(i + 2).get(1) + ")");
		} else if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADV")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("expl:poss")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if (subSentence.get(i).get(7).equals("iobj") && subSentence.get(i + 1).get(7).equals("mark")
				&& subSentence.get(i + 2).get(3).equals("AUX")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if (subSentence.get(i).get(7).equals("iobj") && subSentence.get(i + 1).get(7).equals("expl:pv")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("mark")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		} else if (subSentence.get(i).get(3).equals("PART") && subSentence.get(i + 1).get(3).equals("ADV")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
					+ subSentence.get(i + 2).get(1) + "))");
		}

		return subSentence;
	}

	private ArrayList<ArrayList<String>> thirdCheckIsVerbPart(ArrayList<ArrayList<String>> subSentence, int i) {
		if ((subSentence.get(i).get(1).toLowerCase().equals("se") || subSentence.get(i).get(1).equals("se:"))
				&& subSentence.get(i + 1).get(3).equals("VERB")) {
			subSentence.get(i + 1).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
		} else if (subSentence.get(i).get(3).equals("PART") && subSentence.get(i + 1).get(3).equals("AUX")) {
			subSentence.get(i + 1).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
		} else if (subSentence.get(i).get(3).equals("AUX")
				&& (subSentence.get(i + 1).get(3).equals("VERB") || subSentence.get(i + 1).get(3).equals("AUX"))) {
			subSentence.get(i + 1).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
		} else if (subSentence.get(i).get(7).equals("iobj") && subSentence.get(i + 1).get(3).equals("VERB")) {
			subSentence.get(i + 1).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
		} else if (subSentence.get(1).get(7).equals("expl") && subSentence.get(i + 1).get(3).equals("VERB")) {
			subSentence.get(i + 1).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
		} else if (subSentence.get(1).get(7).equals("expl:pv") && subSentence.get(i + 1).get(3).equals("VERB")) {
			subSentence.get(i + 1).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
		} else if (subSentence.get(i).get(1).contains("-")
				&& (subSentence.get(i + 1).get(3).equals("AUX") || subSentence.get(i + 1).get(3).equals("VERB"))) {
			subSentence.get(i + 1).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("VERB")) {
			subSentence.get(i + 1).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
		} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(3).equals("VERB")) {
			subSentence.get(i + 1).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
		} else if ((subSentence.get(i).get(3).equals("PRON") && subSentence.get(i).get(7).equals("expl:pv"))
				&& subSentence.get(i + 1).get(3).equals("VERB")) {
			subSentence.get(i + 1).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
		} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(3).equals("VERB")) {
			subSentence.get(i + 1).set(1, "(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
		}

		return subSentence;
	}

	public ArrayList<ArrayList<String>> isConjunction(ArrayList<ArrayList<String>> subSentence) {

		for (int i = 0; i < subSentence.size(); i++) {
			while (subSentence.get(i).get(1).contains("și")
					&& (subSentence.get(i + 1).get(i).equals("VERB") || subSentence.get(i + 1).get(i).equals("AUX"))) {
				subSentence.get(i).get(1).replace("și", ",");
			}
		}

		sentence = sentence.replaceAll(" , ", ", ");

		Pattern pattern = Pattern.compile("(([a-zA-Z-ăîțâș]*, ){1,}([a-zA-Z-ăîțâș]* și [a-zA-Z-ăîțâș]*))");
		Matcher matcher = pattern.matcher(sentence);

		while (matcher.find()) {
			String text = matcher.group();
			text = text.replaceAll(",", "");
			text = text.replaceAll("și", "");
			text = text.replaceAll("  ", " ");
			String first = text.split(" ")[0], last = text.split(" ")[text.split(" ").length - 1];

			int indexStart = -1, indexEnd = -1;

			for (int i = 0; i < subSentence.size(); i++) {
				if (subSentence.get(i).get(1).contains(first)) {
					indexStart = i;
				}
				if (subSentence.get(i).get(1).contains(last)) {
					indexEnd = i + 1;
				}
			}
			text = "";

			for (int i = indexStart; i < indexEnd; i++) {
				if (subSentence.get(i).get(1).contains("și")) {
					subSentence.get(i).set(1, "");
				}
				text = text + " " + subSentence.get(i).get(1);
			}
			text = "<" + text.substring(1, text.length()) + ">";

			text = text.replace(" , ", " ");
			text = text.replace("  ", " ");

			try {
				subSentence.get(indexStart).set(1, text);
			} catch (Exception e) {
			}

			for (int i = indexEnd - 1; i > indexStart; i--) {
				subSentence.remove(i);
			}

			ArrayList<Integer> toDelete = new ArrayList<Integer>();
			int i = 0;

			while (i < subSentence.size() - 1) {
				try {
					if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("NOUN")
							&& subSentence.get(i + 2).get(3).equals("CCONJ")
							&& subSentence.get(i + 3).get(3).equals("ADP")
							&& subSentence.get(i + 4).get(3).equals("ADJ")) {
						subSentence.get(i + 1).set(1,
								"((" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ") , ("
										+ subSentence.get(i + 3).get(1) + " " + subSentence.get(i + 4).get(1) + "))");
						toDelete.add(i);
						toDelete.add(i + 2);
						toDelete.add(i + 3);
						toDelete.add(i + 4);
						i += 4;
					}
					if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("NOUN")
							&& subSentence.get(i + 2).get(3).equals("CCONJ")
							&& subSentence.get(i + 3).get(3).equals("ADP")
							&& subSentence.get(i + 4).get(3).equals("NOUN")) {
						subSentence.get(i + 1).set(1,
								"((" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ") , ("
										+ subSentence.get(i + 3).get(1) + " " + subSentence.get(i + 4).get(1) + "))");
						toDelete.add(i);
						toDelete.add(i + 2);
						toDelete.add(i + 3);
						toDelete.add(i + 4);
						i += 4;
					}

					if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(1).equals("și")
							&& subSentence.get(i + 2).get(3).equals("PRON")) {
						subSentence.get(i + 2).set(1,
								"(" + subSentence.get(i).get(1) + " , " + subSentence.get(i + 2).get(1) + ")");
						toDelete.add(i);
						toDelete.add(i + 1);
						i += 2;
					} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("CCONJ")
							&& subSentence.get(i + 2).get(3).equals("NOUN")) {
						subSentence.get(i + 2).set(1,
								"(" + subSentence.get(i).get(1) + " , " + subSentence.get(i + 2).get(1) + ")");
						toDelete.add(i);
						toDelete.add(i + 1);
						i += 2;
					} else if (subSentence.get(i).get(3).equals("ADJ") && subSentence.get(i + 1).get(3).equals("CCONJ")
							&& subSentence.get(i + 2).get(3).equals("ADJ")) {
						subSentence.get(i).set(1,
								"(" + subSentence.get(i).get(1) + " , " + subSentence.get(i + 2).get(1) + ")");
						toDelete.add(i + 1);
						toDelete.add(i + 2);
						i += 2;
					}

					if (subSentence.get(i).get(1).contains("în") && subSentence.get(i + 1).get(1).contains("care")) {
						subSentence.get(i).set(1,
								"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
						toDelete.add(i + 1);
						i += 1;
					} else if (subSentence.get(i).get(1).contains("pentru")
							&& subSentence.get(i + 1).get(1).contains("care")) {
						subSentence.get(i).set(1,
								"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
						toDelete.add(i + 1);
						i += 1;
					}

				} catch (Exception e1) {
					try {
						if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(1).equals("și")
								&& subSentence.get(i + 2).get(3).equals("PRON")) {
							subSentence.get(i + 2).set(1,
									"(" + subSentence.get(i).get(1) + " , " + subSentence.get(i + 2).get(1) + ")");
							toDelete.add(i);
							toDelete.add(i + 1);
							i += 2;
						} else if (subSentence.get(i).get(3).equals("NOUN")
								&& subSentence.get(i + 1).get(3).equals("CCONJ")
								&& subSentence.get(i + 2).get(3).equals("NOUN")) {
							subSentence.get(i + 2).set(1,
									"(" + subSentence.get(i).get(1) + " , " + subSentence.get(i + 2).get(1) + ")");
							toDelete.add(i);
							toDelete.add(i + 1);
							i += 2;
						} else if (subSentence.get(i).get(3).equals("ADJ")
								&& subSentence.get(i + 1).get(3).equals("CCONJ")
								&& subSentence.get(i + 2).get(3).equals("ADJ")) {
							subSentence.get(i).set(1,
									"(" + subSentence.get(i).get(1) + " , " + subSentence.get(i + 2).get(1) + ")");
							toDelete.add(i + 1);
							toDelete.add(i + 2);
							i += 2;
						}

						if (subSentence.get(i).get(1).contains("în")
								&& subSentence.get(i + 1).get(1).contains("care")) {
							subSentence.get(i).set(1,
									"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
							toDelete.add(i + 1);
							i += 1;
						} else if (subSentence.get(i).get(1).contains("pentru")
								&& subSentence.get(i + 1).get(1).contains("care")) {
							subSentence.get(i).set(1,
									"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
							toDelete.add(i + 1);
							i += 1;
						}
					} catch (Exception e2) {
						try {
							if (subSentence.get(i).get(1).contains("în")
									&& subSentence.get(i + 1).get(1).contains("care")) {
								subSentence.get(i).set(1,
										"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
								toDelete.add(i + 1);
								i += 1;
							} else if (subSentence.get(i).get(1).contains("pentru")
									&& subSentence.get(i + 1).get(1).contains("care")) {
								subSentence.get(i).set(1,
										"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
								toDelete.add(i + 1);
								i += 1;
							}
						} catch (Exception e3) {
						}
					}
				}
				i += 1;
			}

			while (toDelete.size() != 0) {
				int delete = toDelete.get(toDelete.size() - 1);
				subSentence.remove(delete);
				toDelete.remove(toDelete.size() - 1);
			}

			i = 0;
			while (i < subSentence.size()) {
				try {
					if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(7).equals("amod")) {
						subSentence.get(i).set(1,
								"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
						toDelete.add(i + 1);
						i += 1;
					} else if ((subSentence.get(i).get(3).equals("ADJ") && subSentence.get(i).get(7).equals("amod"))
							&& (subSentence.get(i + 1).get(3).equals("NOUN")
									&& subSentence.get(i + 1).get(7).equals("nsubj"))) {
						subSentence.get(i).set(1,
								"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
						toDelete.add(i + 1);
						i += 1;
					}
				} catch (Exception e) {
				}
				i += 1;
			}

			while (toDelete.size() != 0) {
				int delete = toDelete.get(toDelete.size() - 1);
				subSentence.remove(delete);
				toDelete.remove(toDelete.size() - 1);
			}

			i = 0;
			while (i < subSentence.size()) {
				try {
					if ((subSentence.get(i).get(3).equals("NOUN") && !subSentence.get(i).get(7).equals("obj"))
							&& subSentence.get(i + 1).get(7).equals("nmod")) {
						subSentence.get(i).set(1,
								"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
						toDelete.add(i + 1);
						i += 1;
					} else if (subSentence.get(i).get(7).equals("case")
							&& subSentence.get(i + 1).get(7).equals("nummod")) {
						subSentence.get(i).set(1,
								"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
						toDelete.add(i + 1);
						i += 1;
					} else if (subSentence.get(i).get(3).equals("NOUN")
							&& subSentence.get(i + 1).get(7).equals("nummod")) {
						subSentence.get(i).set(1,
								"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
						toDelete.add(i + 1);
						i += 1;
					}
				} catch (Exception e) {
				}
				i += 1;
			}

			while (toDelete.size() != 0) {
				int delete = toDelete.get(toDelete.size() - 1);
				subSentence.remove(delete);
				toDelete.remove(toDelete.size() - 1);
			}

		}
		return subSentence;
	}

	public ArrayList<ArrayList<ArrayList<String>>> divideSentence(ArrayList<ArrayList<String>> subSentence) {

		if (subSentence.get(0).get(3).equals("VERB")) {
			subSentence.get(0).set(3, "VB");
		}
		if (subSentence.get(subSentence.size() - 1).get(3).equals("VERB")) {
			subSentence.get(subSentence.size() - 1).set(3, "VB");
		}
		if (subSentence.get(0).get(3).equals("AUX")) {
			subSentence.get(0).set(3, "VP");
		}
		if (subSentence.get(subSentence.size() - 1).get(3).equals("AUX")) {
			subSentence.get(subSentence.size() - 1).set(3, "VP");
		}

		ArrayList<ArrayList<String>> subSentenceTemp = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<ArrayList<String>>> temp = new ArrayList<ArrayList<ArrayList<String>>>();

		int i = 0;
		while (i < subSentence.size()) {
			if ((!subSentence.get(i).get(3).equals("VERB") && (!subSentence.get(i).get(3).equals("AUX")))
					&& subSentence.get(i).get(1).length() != 0) {
				subSentenceTemp.add(subSentence.get(i));
				i += 1;
			} else {
				temp.add(new ArrayList<ArrayList<String>>(subSentenceTemp));
				subSentenceTemp.removeAll(subSentenceTemp);
				subSentenceTemp.add(subSentence.get(i));
				i += 1;
			}
		}
		temp.add(subSentenceTemp);

		if (temp.get(0).get(0).get(3).equals("VB")) {
			temp.get(0).get(0).set(3, "VERB");
		}
		if (temp.get(temp.size() - 1).get(temp.get(temp.size() - 1).size() - 1).get(3).equals("VB")) {
			temp.get(temp.size() - 1).get(temp.get(temp.size() - 1).size() - 1).set(3, "VERB");
		}
		if (temp.get(0).get(0).get(3).equals("VP")) {
			temp.get(0).get(0).set(3, "AUX");
		}
		if (temp.get(temp.size() - 1).get(temp.get(temp.size() - 1).size() - 1).get(3).equals("VP")) {
			temp.get(temp.size() - 1).get(temp.get(temp.size() - 1).size() - 1).set(3, "AUX");
		}

		return temp;
	}

	public ArrayList<ArrayList<String>> addBrackets(ArrayList<ArrayList<String>> subSentence) {
		
		int i = 0, increment = 0;
		ArrayList<String> subSentenceTemp = new ArrayList<String>();
		ArrayList<ArrayList<String>> temp = new ArrayList<ArrayList<String>>();

		while (i < subSentence.size() - 1) {
			try {
				subSentenceTemp = this.getCombFive(subSentence, i);
				increment = 4;
				if (subSentenceTemp == null) {
					try {
						subSentenceTemp = this.getCombFour(subSentence, i);
						increment = 3;
						if (subSentenceTemp == null) {
							try {
								subSentenceTemp = this.getCombThree(subSentence, i);
								increment = 2;
								if (subSentenceTemp == null) {
									subSentenceTemp = this.combTwo(subSentence.get(i), subSentence.get(i + 1));
									increment = 1;
								}
							} catch (Exception combThree) {
								subSentenceTemp = this.combTwo(subSentence.get(i), subSentence.get(i + 1));
								increment = 1;
							}
						}

					} catch (Exception combFour) {
						try {
							subSentenceTemp = this.getCombThree(subSentence, i);
							increment = 2;
							if (subSentenceTemp == null) {
								subSentenceTemp = this.combTwo(subSentence.get(i), subSentence.get(i + 1));
								increment = 1;
							}
						} catch (Exception combThree) {
							subSentenceTemp = this.combTwo(subSentence.get(i), subSentence.get(i + 1));
							increment = 1;
						}
					}
				}
			} catch (Exception combFive) {
				try {
					subSentenceTemp = this.getCombFour(subSentence, i);
					increment = 3;
					if (subSentenceTemp == null) {
						try {
							subSentenceTemp = this.getCombThree(subSentence, i);
							increment = 2;
							if (subSentenceTemp == null) {
								subSentenceTemp = this.combTwo(subSentence.get(i), subSentence.get(i + 1));
								increment = 1;
							}
						} catch (Exception combThree) {
							subSentenceTemp = this.combTwo(subSentence.get(i), subSentence.get(i + 1));
							increment = 1;
						}
					}

				} catch (Exception combFour) {
					try {
						subSentenceTemp = this.getCombThree(subSentence, i);
						increment = 2;
						if (subSentenceTemp == null) {
							subSentenceTemp = this.combTwo(subSentence.get(i), subSentence.get(i + 1));
							increment = 1;
						}
					} catch (Exception combThree) {
						subSentenceTemp = this.combTwo(subSentence.get(i), subSentence.get(i + 1));
						increment = 1;
					}
				}
			}
			i++;
			i += increment;

			temp.add(new ArrayList<String>(subSentenceTemp));
			subSentenceTemp.removeAll(subSentenceTemp);
		}

		try {
			if (subSentence.size() > i) {
				temp.get(temp.size() - 1).set(1, "(" + temp.get(temp.size() - 1).get(1) + " "
						+ subSentence.get(subSentence.size() - 1).get(1) + ")");
			}
		} catch (Exception e) {
			temp = subSentence;
		}

		return temp;
	}

	// metoda ajutatoare addBrackets
	private ArrayList<String> getCombThree(ArrayList<ArrayList<String>> subSentence, int i) {

		ArrayList<String> subSentenceTemp = new ArrayList<String>();

		if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("ADJ")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(7).equals("case") && subSentence.get(i + 1).get(7).equals("nmod")
				&& subSentence.get(i + 2).get(7).equals("nsubj")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 2, i);
		} else if (subSentence.get(i).get(7).equals("fixed") && subSentence.get(i + 1).get(7).equals("obl")
				&& subSentence.get(i + 2).get(7).equals("obj")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("ADJ")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("NOUN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("AUX") && subSentence.get(i + 1).get(3).equals("ADV")
				&& subSentence.get(i + 2).get(3).equals("ADJ")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("PROPN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("DET") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("ADJ")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("NOUN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADJ") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("NOUN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("NOUN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("NOUN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("PRON")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("NOUN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("NOUN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("ADJ") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("ADJ")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("SCONJ") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("DET")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(7).equals("nmod")
				&& subSentence.get(i + 2).get(7).equals("nmod")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("ADJ")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("ADV")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("PROPN") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("PRON")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("AUX") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("NOUN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(7).equals("nmod")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("ADJ")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("ADJ")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("ADJ")
				&& subSentence.get(i + 2).get(3).equals("ADJ")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("NOUN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("AUX") && subSentence.get(i + 1).get(3).equals("ADJ")
				&& subSentence.get(i + 2).get(3).equals("NOUN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("NOUN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADV")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADV")
				&& subSentence.get(i + 2).get(3).equals("NOUN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADV")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("PRON")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("SCONJ")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 2, i);
		} else if (subSentence.get(i).get(3).equals("AUX") && subSentence.get(i + 1).get(3).equals("ADV")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 2, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADV")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("NOUN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("NOUN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADV")
				&& subSentence.get(i + 2).get(3).equals("DET")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("CCONJ") && subSentence.get(i + 1).get(7).equals("nsubj")
				&& subSentence.get(i + 2).get(3).equals("ADV")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADV")
				&& subSentence.get(i + 2).get(3).equals("ADV")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 2, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(7).equals("obj")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 2, i);
		} else if (subSentence.get(i).get(7).equals("mark") && subSentence.get(i + 1).get(7).equals("det")
				&& subSentence.get(i + 2).get(7).equals("nsubj")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("AUX") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("NOUN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADV")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("NUM")
				&& subSentence.get(i + 2).get(3).equals("NOUN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(6).equals(subSentence.get(i + 1).get(6))
				&& subSentence.get(i + 1).get(0).equals(subSentence.get(i + 2).get(0))) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(6).equals(subSentence.get(i + 1).get(0))
				&& subSentence.get(i + 1).get(0).equals(subSentence.get(i + 2).get(6))) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(0).equals(subSentence.get(i + 2).get(6))
				&& subSentence.get(i + 1).get(6).equals(subSentence.get(i + 2).get(0))) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(6).equals(subSentence.get(i + 1).get(0))
				&& subSentence.get(i + 1).get(6).equals(subSentence.get(i + 2).get(6))) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 2, i);
		} else if (subSentence.get(i).get(0).equals(subSentence.get(i + 1).get(6))
				&& subSentence.get(i + 1).get(6).equals(subSentence.get(i + 2).get(6))) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(0).equals(subSentence.get(i).get(6))
				&& subSentence.get(i).get(6).equals(subSentence.get(i + 2).get(6))) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(6).equals(subSentence.get(i + 2).get(6))
				&& subSentence.get(i + 2).get(6).equals(subSentence.get(i + 2).get(0))) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(0).equals(subSentence.get(i + 1).get(6))
				&& subSentence.get(i).get(6).equals(subSentence.get(i + 2).get(0))) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		}

		return subSentenceTemp.size() > 0 ? subSentenceTemp : null;
	}

	// metoda ajutatoare addBrackets
	private ArrayList<String> getCombFour(ArrayList<ArrayList<String>> subSentence, int i) {

		ArrayList<String> subSentenceTemp = new ArrayList<String>();

		if (subSentence.get(i).get(7).equals("obj") && subSentence.get(i + 1).get(7).toLowerCase().equals("root")
				&& subSentence.get(i + 2).get(7).equals("advmod") && subSentence.get(i + 3).get(7).equals("obl")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(3).equals("DET") && subSentence.get(i + 1).get(7).equals("nsubj")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(3).equals("DET") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("DET") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("ADV")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("DET") && subSentence.get(i + 3).get(3).equals("PROPN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("DET") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("CCONJ") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("CCONJ") && subSentence.get(i + 3).get(3).equals("SCONJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("DET") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("PRON") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 4, i);
		} else if (subSentence.get(i).get(3).equals("AUX") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("DET") && subSentence.get(i + 2).get(3).equals("NOUN")
				&& subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("AUX") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("NUM") && subSentence.get(i + 3).get(3).equals("NUM")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("PRON")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("ADV")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("SCONJ")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("ADV")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(3).equals("SCONJ") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("AUX")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("PART") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("ADV") && subSentence.get(i + 3).get(3).equals("AUX")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("PART")
				&& subSentence.get(i + 2).get(3).equals("PRON") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(3).equals("CCONJ") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("PROPN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("PROPN")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if ((subSentence.get(i).get(3).equals("NOUN") || subSentence.get(i).get(3).equals("PRON"))
				&& subSentence.get(i + 1).get(3).equals("AUX") && subSentence.get(i + 2).get(3).equals("AUX")
				&& subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(3).equals("PART") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(7).equals("obj") && subSentence.get(i + 3).get(7).equals("nmod")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("SCONJ") && subSentence.get(i + 1).get(3).equals("PART")
				&& subSentence.get(i + 2).get(3).equals("PRON") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(3).equals("PART") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADJ")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(3).equals("DET") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("ADJ") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("PRON")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("PART") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("ADV")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 5, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("CCONJ")
				&& subSentence.get(i + 2).get(3).equals("DET") && subSentence.get(i + 3).get(3).equals("PRON")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("AUX") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("DET") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("ADJ") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("CCONJ") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("ADJ")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
				&& subSentence.get(i + 1).get(7).equals("case") && subSentence.get(i + 2).get(7).equals("obl")
				&& subSentence.get(i + 3).get(7).equals("nmod")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(7).equals("advmod") && subSentence.get(i + 1).get(7).equals("case")
				&& subSentence.get(i + 2).get(7).equals("det") && subSentence.get(i + 3).get(7).equals("obl")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(7).toLowerCase().equals("root")
				&& subSentence.get(i + 1).get(7).equals("case") && subSentence.get(i + 2).get(7).equals("obl")
				&& subSentence.get(i + 3).get(7).equals("nummod")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADV") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADV") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("SCONJ")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("ADV")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("PRON") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("DET") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("PRON")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("CCONJ")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("PRON")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("DET") && subSentence.get(i + 3).get(3).equals("PRON")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("PRON") && subSentence.get(i + 3).get(3).equals("ADV")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("PRON")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("ADV")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("PRON")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("NUM") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADV") && subSentence.get(i + 3).get(3).equals("VERB")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("DET") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("CCONJ")
				&& subSentence.get(i + 2).get(3).equals("PRON") && subSentence.get(i + 3).get(3).equals("ADV")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("NUM") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("CCONJ")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("DET")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("NUM") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(3).equals("ADJ") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("ADV")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(3).equals("ADJ") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("ADV")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("ADJ") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("ADJ") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("NUM")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADV")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("DET") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("DET") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(3).equals("CCONJ") && subSentence.get(i + 1).get(3).equals("ADV")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("ADV")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("ADV")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("PRON")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADV")
				&& subSentence.get(i + 2).get(3).equals("DET") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NUM")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("PRON") && subSentence.get(i + 3).get(3).equals("AUX")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("ADJ") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("DET") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("PRON")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(6).equals(subSentence.get(i + 1).get(6))
				&& subSentence.get(i + 1).get(6).equals(subSentence.get(i + 2).get(6))
				&& subSentence.get(i + 2).get(6).equals(subSentence.get(i + 3).get(0))) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(0).equals(subSentence.get(i + 1).get(6))
				&& subSentence.get(i + 1).get(6).equals(subSentence.get(i + 2).get(6))
				&& subSentence.get(i + 2).get(6).equals(subSentence.get(i + 3).get(6))) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(0).equals(subSentence.get(i + 1).get(6))
				&& subSentence.get(i + 1).get(6).equals(subSentence.get(i + 2).get(6))
				&& subSentence.get(i + 2).get(0).equals(subSentence.get(i + 3).get(6))) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(6).equals(subSentence.get(i + 1).get(0))
				&& subSentence.get(i + 1).get(6).equals(subSentence.get(i + 2).get(6))
				&& subSentence.get(i + 2).get(6).equals(subSentence.get(i + 3).get(0))) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		}

		return subSentenceTemp.size() > 0 ? subSentenceTemp : null;
	}

	// metoda ajutatoare addBrackets
	private ArrayList<String> getCombFive(ArrayList<ArrayList<String>> subSentence, int i) {

		ArrayList<String> subSentenceTemp = new ArrayList<String>();

		if (subSentence.get(i).get(3).equals("AUX") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("NOUN")
				&& subSentence.get(i + 4).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 1, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("DET") && subSentence.get(i + 3).get(3).equals("NOUN")
				&& subSentence.get(i + 4).get(3).equals("ADV")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 2, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("ADP")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(1), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 3, i);
		} else if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("NOUN")
				&& subSentence.get(i + 4).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 4, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("VERB")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 5, i);
		} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("ADP")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 6, i);
		} else if ((subSentence.get(i).get(3).equals("PRON") || subSentence.get(i).get(7).equals("nsubj"))
				&& subSentence.get(i + 1).get(3).equals("AUX") && subSentence.get(i + 2).get(3).equals("DET")
				&& subSentence.get(i + 3).get(3).equals("NOUN") && subSentence.get(i + 4).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("VERB")
				&& (subSentence.get(i + 4).get(7).equals("ADV") || subSentence.get(i + 4).get(7).equals("NOUN")
						|| subSentence.get(i + 4).get(7).equals("ADJ"))) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 7, i);
		} else if (subSentence.get(i).get(3).equals("DET") && subSentence.get(i + 1).get(3).equals("PROPN")
				&& subSentence.get(i + 2).get(3).equals("PRON") && subSentence.get(i + 3).get(3).equals("AUX")
				&& subSentence.get(i + 4).get(3).equals("VERB")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 6, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("PRON") && subSentence.get(i + 3).get(3).equals("VERB")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 9, i);
		} else if (subSentence.get(i).get(3).equals("AUX") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")
				&& subSentence.get(i + 4).get(7).equals("nmod")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(3).equals("AUX") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("DET") && subSentence.get(i + 3).get(3).equals("NOUN")
				&& subSentence.get(i + 4).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 10, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(7).equals("acl")
				&& subSentence.get(i + 2).get(7).equals("obj") && subSentence.get(i + 3).get(7).equals("case")
				&& subSentence.get(i + 4).get(7).equals("nmod")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 6, i);
		} else if (subSentence.get(i).get(3).equals("DET") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")
				&& subSentence.get(i + 4).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 5, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("ADJ")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("ADP")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 6, i);
		} else if ((subSentence.get(i).get(3).equals("PRON") || subSentence.get(i).get(7).equals("nsubj"))
				&& (subSentence.get(i + 1).get(3).equals("AUX") || subSentence.get(i + 1).get(3).equals("VERB"))
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")
				&& subSentence.get(i + 4).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("PRON") && subSentence.get(i + 3).get(3).equals("VERB")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 10, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADV") && subSentence.get(i + 3).get(3).equals("DET")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("CCONJ")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("ADP")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 10, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")
				&& subSentence.get(i + 4).get(3).equals("NUM")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 10, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("DET")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("DET") && subSentence.get(i + 3).get(3).equals("NOUN")
				&& subSentence.get(i + 4).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("ADP")
				&& subSentence.get(i + 4).get(3).equals("ADV")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 6, i);
		} else if (subSentence.get(i).get(7).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADV") && subSentence.get(i + 3).get(3).equals("VERB")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 6, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("CCONJ")
				&& subSentence.get(i + 2).get(3).equals("CCONJ") && subSentence.get(i + 3).get(7).equals("nsubj")
				&& subSentence.get(i + 4).get(3).equals("VERB")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADP")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 3, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADP")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("NOUN")
				&& subSentence.get(i + 4).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADP")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 10, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADP")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 1, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADP")
				&& subSentence.get(i + 4).get(3).equals("PRON")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADP")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("ADP")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 3, i);
		}

		return subSentenceTemp.size() > 0 ? subSentenceTemp : null;
	}

	private ArrayList<String> combFive(ArrayList<String> lst1, ArrayList<String> lst2, ArrayList<String> lst3,
			ArrayList<String> lst4, ArrayList<String> lst5, int comb, int i) {

		ArrayList<String> temp = new ArrayList<String>();

		temp.add(String.valueOf(
				(Double.parseDouble(lst1.get(0)) + Double.parseDouble(lst2.get(0)) + Double.parseDouble(lst3.get(0))
						+ Double.parseDouble(lst4.get(0)) + Double.parseDouble(lst5.get(0))) / 5));

		if (comb == 1) {
			temp.add("((" + lst1.get(1) + " (" + lst2.get(1) + " " + lst3.get(1) + ")) (" + lst4.get(1) + " "
					+ lst5.get(1) + "))");
		} else if (comb == 2) {
			temp.add("(" + lst1.get(1) + " (" + lst2.get(1) + " ((" + lst3.get(1) + " " + lst4.get(1) + ") "
					+ lst5.get(1) + ")))");
		} else if (comb == 3) {
			temp.add("(" + lst1.get(1) + " ((" + lst2.get(1) + " " + lst3.get(1) + ") (" + lst4.get(1) + " "
					+ lst5.get(1) + ")))");
		} else if (comb == 4) {
			temp.add("((" + lst1.get(1) + " (" + lst2.get(1) + " " + lst3.get(1) + ")) (" + lst4.get(1) + " "
					+ lst5.get(1) + "))");
		} else if (comb == 5) {
			temp.add("((" + lst1.get(1) + " (" + lst2.get(1) + " (" + lst3.get(1) + " " + lst4.get(1) + ")))"
					+ lst5.get(1) + ")");
		} else if (comb == 6) {
			temp.add("((" + lst1.get(1) + " " + lst2.get(1) + ") (" + lst3.get(1) + " (" + lst4.get(1) + " "
					+ lst5.get(1) + ")))");
		} else if (comb == 7) {
			temp.add("((" + lst1.get(1) + " ((" + lst2.get(1) + " " + lst3.get(1) + ") " + lst4.get(1) + ")) "
					+ lst5.get(1) + ")");
		} else if (comb == 8) {
			temp.add("(" + lst1.get(1) + " (" + lst2.get(1) + " (" + lst3.get(1) + " (" + lst4.get(1) + " "
					+ lst5.get(1) + "))))");
		} else if (comb == 9) {
			temp.add("(((" + lst1.get(1) + " " + lst2.get(1) + ") (" + lst3.get(1) + " " + lst4.get(1) + ")) "
					+ lst5.get(1) + ")");
		} else if (comb == 10) {
			temp.add("((" + lst1.get(1) + " " + lst2.get(1) + ") (" + lst3.get(1) + " (" + lst4.get(1) + " "
					+ lst5.get(1) + ")))");
		}

		temp.add(lst1.get(2));
		temp.add(lst1.get(3));
		temp.add(lst1.get(4));
		temp.add(lst1.get(5));
		temp.add(String.valueOf(
				(Double.parseDouble(lst1.get(6)) + Double.parseDouble(lst2.get(6)) + Double.parseDouble(lst3.get(6))
						+ Double.parseDouble(lst4.get(6)) + Double.parseDouble(lst5.get(6))) / 5));
		temp.add(lst1.get(7));
		temp.add(lst1.get(8));
		temp.add(lst1.get(9));
		
		return temp;
	}

	private ArrayList<String> combFour(ArrayList<String> lst1, ArrayList<String> lst2, ArrayList<String> lst3,
			ArrayList<String> lst4, int comb, int i) {

		ArrayList<String> temp = new ArrayList<String>();

		temp.add(String.valueOf((Double.parseDouble(lst1.get(0)) + Double.parseDouble(lst2.get(0))
				+ Double.parseDouble(lst3.get(0)) + Double.parseDouble(lst4.get(0))) / 4));

		if (comb == 1) {
			temp.add("(" + lst1.get(1) + " ((" + lst2.get(1) + " " + lst3.get(1) + ") " + lst4.get(1) + "))");
		} else if (comb == 2) {
			temp.add("(" + lst1.get(1) + " (" + lst2.get(1) + " (" + lst3.get(1) + " " + lst4.get(1) + ")))");
		} else if (comb == 3) {
			temp.add("((" + lst1.get(1) + " " + lst2.get(1) + ") (" + lst3.get(1) + " " + lst4.get(1) + "))");
		} else if (comb == 4) {
			temp.add("(((" + lst1.get(1) + " " + lst2.get(1) + ")" + lst3.get(1) + " ) " + lst4.get(1) + ")");
		} else if (comb == 5) {
			temp.add("((" + lst1.get(1) + " (" + lst2.get(1) + " " + lst3.get(1) + ")) " + lst4.get(1) + ")");
		}

		temp.add(lst1.get(2));
		temp.add(lst1.get(3));
		temp.add(lst1.get(4));
		temp.add(lst1.get(5));
		temp.add(String.valueOf((Double.parseDouble(lst1.get(6)) + Double.parseDouble(lst2.get(6))
				+ Double.parseDouble(lst3.get(6)) + Double.parseDouble(lst4.get(6))) / 4));
		temp.add(lst1.get(7));
		temp.add(lst1.get(8));
		temp.add(lst1.get(9));
		
		return temp;
	}

	private ArrayList<String> combThree(ArrayList<String> lst1, ArrayList<String> lst2, ArrayList<String> lst3,
			int comb, int i) {

		ArrayList<String> temp = new ArrayList<String>();

		temp.add(String.valueOf(
				(Double.parseDouble(lst1.get(0)) + Double.parseDouble(lst2.get(0)) + Double.parseDouble(lst3.get(0)))
						/ 3));

		if (comb == 1) {
			temp.add("(" + lst1.get(1) + " (" + lst2.get(1) + " " + lst3.get(1) + "))");
		} else if (comb == 2) {
			temp.add("((" + lst1.get(1) + " " + lst2.get(1) + ") " + lst3.get(1) + ")");
		}

		temp.add(lst1.get(2));
		temp.add(lst1.get(3));
		temp.add(lst1.get(4));
		temp.add(lst1.get(5));
		temp.add(String.valueOf(
				(Double.parseDouble(lst1.get(6)) + Double.parseDouble(lst2.get(6)) + Double.parseDouble(lst3.get(6)))
						/ 3));
		temp.add(lst1.get(7));
		temp.add(lst1.get(8));
		temp.add(lst1.get(9));
		
		return temp;
	}

	private ArrayList<String> combTwo(ArrayList<String> lst1, ArrayList<String> lst2) {

		ArrayList<String> temp = new ArrayList<String>();

		temp.add(String.valueOf((Double.parseDouble(lst1.get(0)) + Double.parseDouble(lst2.get(0))) / 2));

		temp.add("(" + lst1.get(1) + " " + lst2.get(1) + ")");

		temp.add(lst1.get(2));
		temp.add(lst1.get(3));
		temp.add(lst1.get(4));
		temp.add(lst1.get(5));
		temp.add(String.valueOf((Double.parseDouble(lst1.get(6)) + Double.parseDouble(lst2.get(6))) / 2));
		temp.add(lst1.get(7));
		temp.add(lst1.get(8));
		temp.add(lst1.get(9));
		
		return temp;
	}

	public ArrayList<ArrayList<String>> setFinalBrackets(ArrayList<ArrayList<String>> subSentence) {

		// ultimul constituent are media dependentie mare
		// si se scade putin
		subSentence.get(subSentence.size() - 1).set(0,
				String.valueOf(Double.parseDouble(subSentence.get(subSentence.size() - 1).get(0)) - 1.1));
		subSentence.get(subSentence.size() - 1).set(6,
				String.valueOf(Double.parseDouble(subSentence.get(subSentence.size() - 1).get(6)) - 1.1));

		// mediile pentru fiecare constituent
		ArrayList<ArrayList<String>> lista = new ArrayList<ArrayList<String>>();
		for (ArrayList<String> item : subSentence) {
			ArrayList<String> temp = new ArrayList<String>();
			temp.add(String.valueOf((Double.parseDouble(item.get(0)) + Double.parseDouble(item.get(6))) / 2));
			temp.add(item.get(1));
			temp.add(item.get(2));
			temp.add(item.get(3));
			temp.add(item.get(4));
			temp.add(item.get(5));
			temp.add(String.valueOf((Double.parseDouble(item.get(0)) + Double.parseDouble(item.get(6))) / 2));
			temp.add(item.get(7));
			temp.add(item.get(8));
			temp.add(item.get(9));
			lista.add(new ArrayList<String>(temp));
		}

		for (int item = 0; item < lista.size() - 1; item++) {
			// scaderea fiecarui element pentru a primit o lista de diferente
			ArrayList<Double> difference = new ArrayList<Double>();
			for (int i = 0; i < lista.size() - 1; i++) {
				difference.add(Double.parseDouble(lista.get(i + 1).get(0)) - Double.parseDouble(lista.get(i).get(0)));
			}
			// cauta pozitia elementului cel mai apropiat
			Double position = difference.get(0);
			int index = 0;
			for (int i = 1; i < difference.size(); i++) {
				if (position > difference.get(i)) {
					index = i;
					position = difference.get(i);
				}
			}
			lista.get(index).set(0, String.valueOf(
					(Double.parseDouble(lista.get(index).get(0)) + Double.parseDouble(lista.get(index + 1).get(0)))
							/ 2));
			lista.get(index).set(1, "(" + lista.get(index).get(1) + " " + lista.get(index + 1).get(1) + ")");
			lista.remove(index + 1);
		}

		lista.get(0).add(lista.get(0).get(0));

		return lista;
	}

	public String getLemma() {
		String lemma_txt = "";
		for (int i = 0; i < sentencePunctilog.size(); i++) {
			lemma_txt += sentencePunctilog.get(i).get(2) + " ";
		}
		return lemma_txt;
	}

	public String getSentence() {
		return sentence;
	}

	public int getLengthSentencePunctilog() {
		return sentencePunctilog.size();
	}

	public ArrayList<ArrayList<String>> getSentencePunctilog() {
		return this.sentencePunctilog;
	}

	public ArrayList<String> getSentencePunctilogLine(int line) {
		return this.sentencePunctilog.get(line);
	}

	public String getSentencePunctilogField(int line, int field) {
		return this.sentencePunctilog.get(line).get(field);
	}

}

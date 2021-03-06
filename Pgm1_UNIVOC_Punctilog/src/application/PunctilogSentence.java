package application;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

import edu.stanford.nlp.pipeline.CoNLLUReader;

public class PunctilogSentence {

	private ArrayList<ArrayList<String>> sentencePunctilog = new ArrayList<ArrayList<String>>();
	private String sentence = "";

	public PunctilogSentence(CoNLLUReader.CoNLLUSentence conllSent) {

		// memorarea informatiilor despre cuvintele propozitiei
		for (String tokenLine : conllSent.tokenLines) {
			ArrayList<String> word = new ArrayList<String>();
			String[] splits = tokenLine.split("\t");
			word.add(splits[CoNLLUReader.CoNLLU_IndexField]); // index 1
			word.add(splits[CoNLLUReader.CoNLLU_WordField]); // text 2
			word.add(splits[CoNLLUReader.CoNLLU_LemmaField]); // lemma
			word.add(splits[CoNLLUReader.CoNLLU_UPOSField]); // upos 4
			word.add(splits[CoNLLUReader.CoNLLU_XPOSField]); // xpos
			word.add(splits[CoNLLUReader.CoNLLU_RelnField]); // deprel 5
			word.add(splits[CoNLLUReader.CoNLLU_GovField]); // dependency 3

			if (word.get(1).toString().strip().equals("\'")) {
				word.set(1, "\"");
				word.set(2, "\"");
			}
			if (word.get(1).toString().strip().equals("?")) {
				word.set(1, ".");
				word.set(2, ".");
			}
			sentencePunctilog.add(word);
		}

		if (sentencePunctilog.get(sentencePunctilog.size() - 1).get(1).equals(".")
				|| sentencePunctilog.get(sentencePunctilog.size() - 1).get(1).equals(",")
				|| sentencePunctilog.get(sentencePunctilog.size() - 1).get(1).equals(":")
				|| sentencePunctilog.get(sentencePunctilog.size() - 1).get(1).equals(";")
				|| sentencePunctilog.get(sentencePunctilog.size() - 1).get(1).equals("?")
				|| sentencePunctilog.get(sentencePunctilog.size() - 1).get(1).equals("!")) {
			sentencePunctilog.remove(sentencePunctilog.size() - 1);
		}

		// unirea cuvintelor despartite de cratima
		for (int i = 0; i < sentencePunctilog.size(); i++) {
			String temp = sentencePunctilog.get(i).get(1).toString();
			
			if (temp.substring(temp.length() - 1).equals("-")) {
				sentence = sentence + temp + sentencePunctilog.get(i + 1).get(1) + " ";
				sentencePunctilog.get(i).set(1,
						sentencePunctilog.get(i).get(1).toString() + sentencePunctilog.get(i + 1).get(1).toString());
				sentencePunctilog.remove(i + 1);
			} else if (temp.substring(0, 1).equals("-")) {
				sentence = sentence.substring(0, sentence.length() - 1) + temp + " ";
				sentencePunctilog.get(i - 1).set(1,
						sentencePunctilog.get(i - 1).get(1).toString() + sentencePunctilog.get(i).get(1).toString());
				sentencePunctilog.remove(i);
			} else {
				sentence = sentence + sentencePunctilog.get(i).get(1) + " ";
			}
		}

		// setarea head pentru verb root id-ul sau
		for (int i = 0; i < sentencePunctilog.size() - 1; i++) {
			if (sentencePunctilog.get(i).get(3).toString().equals("VERB")
					&& Integer.parseInt((String) sentencePunctilog.get(i).get(6)) == 0) {
				sentencePunctilog.get(i).set(6, String.valueOf(i + 2));
			}
		}
	}

	// VERIFICAREA EXPRESIILOR IN SENTENCE SI MARCAREA ACESTORA < >
	public void isExpression() {
		
		while (true) {
			ArrayList<Integer> indexExpression = new ArrayList<Integer>();
			
			try {
				File myObj = new File("all.txt");
				Scanner myReader = new Scanner(myObj);

				while (myReader.hasNextLine()) {
					String lines = myReader.nextLine();
					lines = lines.strip();
					if (this.getLemma().contains(" " + lines + " ")) {
						String[] line = lines.split(" ");
						String[] lemma = this.getLemma().split(" ");
						String[] words = this.getSentence().split(" ");
						
						try {
							
							for (int i = 0; i < line.length; i++) {
								for (int j = 0; j < lemma.length; j++) {
									if (line[i].equals(lemma[j]) && line[i + 1].equals(lemma[j + 1])) {
										int tempI = i, tempJ = j;
										while (line[tempI].equals(lemma[tempJ])
												&& line[tempI + 1].equals(lemma[tempJ + 1])) {
											indexExpression.add(tempJ);
											tempI++;
											tempJ++;
										}
										i = line.length;
										j = lemma.length;
									}
								}
							}
						} catch (Exception e) {
						}
						String temp = "";

						for (int i = 0; i < words.length; i++) {
							if (indexExpression.contains(i)) {
								temp += words[i] + "??";
							} else {
								temp += words[i] + " ";
							}
						}
						this.sentence = temp.substring(0, temp.length() - 1); // eliminarea ultimului spatiu
					}
				}
				myReader.close();
			} catch (Exception e) {
			}

			// inserarea < > in sentence
			if (indexExpression.size() != 0) {
				indexExpression.add(indexExpression.get(indexExpression.size() - 1) + 1);
				String index = "", text = "<", lemma = "", upos = "", xpos = "", deprel = "";
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
				
				for (int i : indexExpression) {
					if (sentencePunctilog.get(i).get(3).equals("VERB")
							|| sentencePunctilog.get(i).get(3).equals("AUX")) {
						index = sentencePunctilog.get(i).get(0);
						lemma += sentencePunctilog.get(i).get(2) + " ";
						upos = sentencePunctilog.get(i).get(3);
						xpos = sentencePunctilog.get(i).get(4);
						deprel = sentencePunctilog.get(i).get(5);
						j = i;
						break;
					} else if (sentencePunctilog.get(i).get(3).equals("NOUN")
							|| sentencePunctilog.get(i).get(3).equals("ADJ")) {
						index = sentencePunctilog.get(i).get(0);
						lemma += sentencePunctilog.get(i).get(2) + " ";
						upos = sentencePunctilog.get(i).get(3);
						xpos = sentencePunctilog.get(i).get(4);
						deprel = sentencePunctilog.get(i).get(5);
					}
					j = i;
				}
				if (upos.equals("")) {
					index = sentencePunctilog.get(j).get(0);
					lemma += sentencePunctilog.get(j).get(2) + " ";
					upos = sentencePunctilog.get(j).get(3);
					xpos = sentencePunctilog.get(j).get(4);
					deprel = sentencePunctilog.get(j).get(5);
				}
				j = indexExpression.get(0);
				sentencePunctilog.get(j).set(0, index);
				sentencePunctilog.get(j).set(1, text);
				sentencePunctilog.get(j).set(2, lemma.substring(0, lemma.length() - 1));
				sentencePunctilog.get(j).set(3, upos);
				sentencePunctilog.get(j).set(4, xpos);
				sentencePunctilog.get(j).set(5, deprel);
				sentencePunctilog.get(j).set(6, String.valueOf(dependency / dependencyCount));

				int countDeleted = 0;
				
				for (int k : indexExpression.subList(1, indexExpression.size())) {
					// revenirea la noul index al elementului care trebuie sters
					k -= countDeleted;
					sentencePunctilog.remove(k);
					countDeleted += 1;
				}
			} else {
				break;
			}
		}
	}

	public ArrayList<ArrayList<String>> isDirectSpeech(ArrayList<ArrayList<String>> sentencesPunctilog) {

		boolean openBrackets = false;

		for (int i = 0; i < sentencesPunctilog.size(); i++) {
			if (sentencesPunctilog.get(i).get(1).equals("\"") && !openBrackets) {
				sentencesPunctilog.get(i + 1).set(1, "[" + sentencesPunctilog.get(i + 1).get(1));
				openBrackets = true;
			} else if (sentencesPunctilog.get(i).get(1).equals("\"") && openBrackets) {
				sentencesPunctilog.get(i - 1).set(1, sentencesPunctilog.get(i - 1).get(1) + "]");
				openBrackets = false;
			}
		}
		ArrayList<ArrayList<String>> list = new ArrayList<ArrayList<String>>();

		sentencesPunctilog.forEach(s -> {
			if (!s.get(1).equals("\"")) {
				list.add(s);
			}
		});

		ArrayList<Integer> indexToDelete = new ArrayList<Integer>();
		ArrayList<String> pos = new ArrayList<String>();
		ArrayList<ArrayList<Integer>> listToDelete = new ArrayList<ArrayList<Integer>>();
		String directSpeech = "";

		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).get(1).substring(0, 1).equals("[")) {

				for (int j = i; j < list.size(); j++) {

					indexToDelete.add(j);

					pos.add(list.get(j).get(0));
					directSpeech += " " + list.get(j).get(1);
					pos.add(list.get(j).get(2));
					pos.add(list.get(j).get(3));
					pos.add(list.get(j).get(4));
					pos.add(list.get(j).get(5));
					pos.add(list.get(j).get(6));

					if (list.get(j).get(1).charAt(list.get(j).get(1).length() - 1) == ']') {
						indexToDelete.remove(0);
						listToDelete.add(indexToDelete);
						list.get(i).set(1, directSpeech.substring(1));

						// se parcurge doar pe indexurile UPOS, restul campurilor se extrag dupa
						// indexuri
						for (int k = 0; k < pos.size(); k += 6) {

							if (pos.get(k).equals("VERB")) {
								list.get(i).set(0, pos.get(k));
								list.get(i).set(2, pos.get(k + 1));
								list.get(i).set(3, "VERB");
								list.get(i).set(4, pos.get(k + 3));
								list.get(i).set(5, pos.get(k + 4));
								list.get(i).set(6, pos.get(k + 5));
								break;
							} else if (pos.get(k).equals("NOUN")) {
								list.get(i).set(0, pos.get(k));
								list.get(i).set(2, pos.get(k + 1));
								list.get(i).set(3, "NOUN");
								list.get(i).set(4, pos.get(k + 3));
								list.get(i).set(5, pos.get(k + 4));
								list.get(i).set(6, pos.get(k + 5));
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

		sentencePunctilog.clear();
		list.forEach(line -> {
			sentencePunctilog.add(line);
		});

		return list;
	}

	public ArrayList<ArrayList<String>> isModifier(ArrayList<ArrayList<String>> subSentence) {

		for (int i = 0; i < subSentence.size() - 1; i++) {

			if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
//			} else if (subSentence.get(i+1).get(3).equals("VERB")) {
//				subSentence.get(i+1).set(1, ":" + subSentence.get(i+1).get(1));
			} else if (subSentence.get(i + 1).get(3).equals("AUX")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("obl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nmod") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1));
			} else if (subSentence.get(i).get(5).equals("cc") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
//			} else if (subSentence.get(i).get(5).equals("aux") && subSentence.get(i+1).get(5).equals("root")) {
//				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
//			}
			} else if (subSentence.get(i).get(5).equals("obl") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("obl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obl") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("nmod:pmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nmod") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("obj") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("nsubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("conj") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("cop") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1));
			} else if (subSentence.get(i).get(5).equals("aux:pass") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advcl") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(5).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nummod") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl:pv") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("punct") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("nmod:agent")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("advmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl:pass") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(5).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obj") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("aux") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("nummod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("acl") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nmod") && subSentence.get(i + 1).get(5).equals("nummod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("ccomp") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("conj") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("aux") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("aux") && subSentence.get(i + 1).get(5).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obl") && subSentence.get(i + 1).get(5).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("amod") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("nsubj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("expl:pv") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obl") && subSentence.get(i + 1).get(5).equals("nummod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nsubj:pass") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nmod:pmod") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("conj") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(5).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(5).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nummod") && subSentence.get(i + 1).get(5).equals("case")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nummod") && subSentence.get(i + 1).get(5).equals("obl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("amod") && subSentence.get(i + 1).get(5).equals("nsubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nsubj:pass") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("expl:pv") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("iobj") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("iobj") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("nmod:pmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obj") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("nummod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("nmod:tmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nmod") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nmod:pmod") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("amod") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nmod") && subSentence.get(i + 1).get(5).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("aux:pass") && subSentence.get(i + 1).get(5).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("amod") && subSentence.get(i + 1).get(5).equals("obl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("appos") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("ccomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("conj") && subSentence.get(i + 1).get(5).equals("flat")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("expl:pv") && subSentence.get(i + 1).get(5).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("conj") && subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("iobj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("xcomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nmod:agent") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("ccomp") && subSentence.get(i + 1).get(5).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("conj") && subSentence.get(i + 1).get(5).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("advcl") && subSentence.get(i + 1).get(1).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("obl") && subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("nsubj:pass")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("acl") && subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nummod") && subSentence.get(i + 1).get(5).equals("nsubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obj") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("aux") && subSentence.get(i + 1).get(5).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("parataxis") && subSentence.get(i + 1).get(5).equals("nsubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl:pv") && subSentence.get(i + 1).get(5).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nmod") && subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("obj") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("case")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("acl") && subSentence.get(i + 1).get(5).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("amod") && subSentence.get(i + 1).get(5).equals("iobj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("obl") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("nsubj:pass")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("advmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("cop") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advcl") && subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("det")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nummod") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nmod:tmod")
					&& subSentence.get(i + 1).get(5).equals("nummod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("iobj") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("iobj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(5).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obj") && subSentence.get(i + 1).get(5).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("amod") && subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("nummod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("acl") && subSentence.get(i + 1).get(5).equals("iobj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("cop") && subSentence.get(i + 1).get(5).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("aux:pass") && subSentence.get(i + 1).get(5).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("cop") && subSentence.get(i + 1).get(5).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("nmod:agent")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(5).equals("ccomp:pmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("amod") && subSentence.get(i + 1).get(5).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("parataxis") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("appos")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("xcomp") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("appos") && subSentence.get(i + 1).get(1).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl:pass") && subSentence.get(i + 1).get(5).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nummod") && subSentence.get(i + 1).get(5).equals("goeswith")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nmod:agent") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("cop") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("goeswith")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("obl") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("xcomp") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("iobj") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("nsubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("conj") && subSentence.get(i + 1).get(5).equals("xcomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("aux:pass") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl:poss") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obl") && subSentence.get(i + 1).get(5).equals("flat")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("ccomp:pmod") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("obl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("conj") && subSentence.get(i + 1).get(5).equals("nsubj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("acl") && subSentence.get(i + 1).get(5).equals("nsubj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("advcl") && subSentence.get(i + 1).get(5).equals("expl:pv")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("iobj") && subSentence.get(i + 1).get(5).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obj") && subSentence.get(i + 1).get(5).equals("advcl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advcl") && subSentence.get(i + 1).get(5).equals("iobj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("csubj") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("cc") && subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("ccomp") && subSentence.get(i + 1).get(5).equals("nsubj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("expl:pass") && subSentence.get(i + 1).get(5).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nummod") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("acl") && subSentence.get(i + 1).get(5).equals("ccomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("nmod:tmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obj") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("aux") && subSentence.get(i + 1).get(5).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("aux:pass") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("aux") && subSentence.get(i + 1).get(5).equals("advcl:tcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("iobj") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("acl") && subSentence.get(i + 1).get(5).equals("xcomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("expl:poss") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl:poss") && subSentence.get(i + 1).get(5).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl:poss") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("conj") && subSentence.get(i + 1).get(5).equals("iobj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nsubj:pass") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("expl") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl:pass") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(5).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(5).equals("parataxis")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nummod:tmod")
					&& subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nmod") && subSentence.get(i + 1).get(5).equals("punct")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nummod") && subSentence.get(i + 1).get(5).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nummod") && subSentence.get(i + 1).get(5).equals("punct")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nmod:tmod") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("advcl:tcl") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("obj") && subSentence.get(i + 1).get(5).equals("nummod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("obl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("advmod")
					&& subSentence.get(i + 1).get(5).equals("parataxis")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl") && subSentence.get(i + 1).get(5).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("aux:pass") && subSentence.get(i + 1).get(5).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advcl") && subSentence.get(i + 1).get(5).equals("nsubj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("expl:impres")
					&& subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("xcomp") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nmod:agent") && subSentence.get(i + 1).get(5).equals("flat")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nmod") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("conj") && subSentence.get(i + 1).get(5).equals("ccomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("expl") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("parataxis") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl") && subSentence.get(i + 1).get(5).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advcl") && subSentence.get(i + 1).get(5).equals("ccomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("aux") && subSentence.get(i + 1).get(5).equals("parataxis")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("appos") && subSentence.get(i + 1).get(5).equals("punct")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nummod") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("expl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("ccomp") && subSentence.get(i + 1).get(5).equals("xcomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nmod") && subSentence.get(i + 1).get(5).equals("case")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("expl:pv") && subSentence.get(i + 1).get(5).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("iobj") && subSentence.get(i + 1).get(5).equals("parataxis")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advcl") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("amod") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advmod")
					&& subSentence.get(i + 1).get(5).equals("nsubj:pass")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("aux") && subSentence.get(i + 1).get(5).equals("csubj:pass")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl:poss") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nummod")
					&& subSentence.get(i + 1).get(5).equals("nsubj:pass")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("aux:pass")
					&& subSentence.get(i + 1).get(5).equals("parataxis")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("iobj") && subSentence.get(i + 1).get(5).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("iobj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("appos")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obl") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("csubj") && subSentence.get(i + 1).get(5).equals("iobj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("expl:poss") && subSentence.get(i + 1).get(5).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obl") && subSentence.get(i + 1).get(5).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("obl") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("ccomp") && subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("advcl") && subSentence.get(i + 1).get(5).equals("xcomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nmod") && subSentence.get(i + 1).get(5).equals("nsubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nmod") && subSentence.get(i + 1).get(5).equals("goeswith")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nmod:pmod") && subSentence.get(i + 1).get(5).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("obj") && subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nummod") && subSentence.get(i + 1).get(5).equals("nummod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("case")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(5).equals("obl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("advcl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("cc:personaj")
					&& subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("appos") && subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("nummod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("ccomp") && subSentence.get(i + 1).get(5).equals("obl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("iobj") && subSentence.get(i + 1).get(5).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl") && subSentence.get(i + 1).get(5).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nummod") && subSentence.get(i + 1).get(5).equals("compound")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("iobj") && subSentence.get(i + 1).get(5).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl:pv")
					&& subSentence.get(i + 1).get(5).equals("parataxis")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("cop")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nmod:pmod")
					&& subSentence.get(i + 1).get(5).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("iobj") && subSentence.get(i + 1).get(5).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("csubj") && subSentence.get(i + 1).get(5).equals("nsubj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("cop") && subSentence.get(i + 1).get(5).equals("parataxis")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("appos") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("parataxis")
					&& subSentence.get(i + 1).get(5).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("obj") && subSentence.get(i + 1).get(5).equals("#")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("ccomp") && subSentence.get(i + 1).get(5).equals("iobj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("parataxis")
					&& subSentence.get(i).get(5).equals("nsubj:pass")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nummod")
					&& subSentence.get(i + 1).get(5).equals("nummod:pmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obl") && subSentence.get(i + 1).get(5).equals("punct")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("csubj")
					&& subSentence.get(i + 1).get(5).equals("nsubj:pass")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("csubj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("advcl")
					&& subSentence.get(i + 1).get(5).equals("nsubj:pass")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("advcl") && subSentence.get(i + 1).get(5).equals("obl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("aux") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl:impress")
					&& subSentence.get(i + 1).get(5).equals("advcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("det")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("aux:pass")
					&& subSentence.get(i + 1).get(5).equals("advcl:tcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("amod") && subSentence.get(i + 1).get(5).equals("iobj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("iobj") && subSentence.get(i + 1).get(5).equals("flat")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("conj") && subSentence.get(i + 1).get(5).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("parataxis") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("acl") && subSentence.get(i + 1).get(5).equals("nsubj:pass")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("parataxis") && subSentence.get(i + 1).get(5).equals("expl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("amod") && subSentence.get(i + 1).get(5).equals("nsubj:pass")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nsubj:pass") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(5).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obl") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("amod") && subSentence.get(i + 1).get(5).equals("nmod:agent")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obj") && subSentence.get(i + 1).get(5).equals("ccomp:pmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("appos") && subSentence.get(i + 1).get(5).equals("nummod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("parataxis") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nsubj:pass") && subSentence.get(i + 1).get(5).equals("flat")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("acl") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(5).equals("ccomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("conj") && subSentence.get(i + 1).get(5).equals("advcl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nmod:agent") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("conj") && subSentence.get(i + 1).get(5).equals("nummod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("cop") && subSentence.get(i + 1).get(5).equals("csubj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nummod") && subSentence.get(i + 1).get(5).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("appos") && subSentence.get(i + 1).get(5).equals("goeswith")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(5).equals("advcl:tcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("goeswith")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("parataxis") && subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("aux") && subSentence.get(i + 1).get(5).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("case") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obj") && subSentence.get(i + 1).get(5).equals("parataxis")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("parataxis") && subSentence.get(i + 1).get(5).equals("xcomp")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("fixed") && subSentence.get(i + 1).get(5).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nmod:pmod") && subSentence.get(i + 1).get(5).equals("acl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl:pv")
					&& subSentence.get(i + 1).get(5).equals("advcl:tcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("xcomp") && subSentence.get(i + 1).get(5).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("conj") && subSentence.get(i + 1).get(5).equals("expl:poss")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("expl:poss")
					&& subSentence.get(i + 1).get(5).equals("orphan")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obj") && subSentence.get(i + 1).get(5).equals("advcl:tcl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("amod") && subSentence.get(i + 1).get(5).equals("nmod:pmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nmod") && subSentence.get(i + 1).get(5).equals("obl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("cc") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("flat") && subSentence.get(i + 1).get(5).equals("nummod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("amod") && subSentence.get(i + 1).get(5).equals("xcomp")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("aux:pass") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("aux") && subSentence.get(i + 1).get(5).equals("appos")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("appos") && subSentence.get(i + 1).get(5).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("flat") && subSentence.get(i + 1).get(5).equals("flat")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nummod") && subSentence.get(i + 1).get(5).equals("appos")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nmod:pmod") && subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(5).equals("amod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("expl:impres")
					&& subSentence.get(i + 1).get(5).equals("conj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nmod:agent") && subSentence.get(i + 1).get(5).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("parataxis") && subSentence.get(i + 1).get(5).equals("iobj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("cc:preconj") && subSentence.get(i + 1).get(5).equals("obj")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nsubj:pass")
					&& subSentence.get(i + 1).get(5).equals("fixed")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("acl") && subSentence.get(i + 1).get(5).equals("csubj")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("expl:pass") && subSentence.get(i + 1).get(5).equals("obl")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obl:pmod") && subSentence.get(i + 1).get(5).equals("nmod")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("ADJ")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("amod") && subSentence.get(i + 1).get(5).equals("root")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("det") && subSentence.get(i + 1).get(5).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(5).equals("aux")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("mark")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("advcl") && subSentence.get(i + 1).get(5).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("aux") && subSentence.get(i + 1).get(5).equals("aux")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("iobj") && subSentence.get(i + 1).get(5).equals("aux")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("obj") && subSentence.get(i + 1).get(5).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("advmod")
					&& subSentence.get(i + 1).get(5).equals("expl:pass")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nsubj:pass") && subSentence.get(i + 1).get(5).equals("det")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("cop") && subSentence.get(i + 1).get(5).equals("advmod")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("case")) {
				subSentence.get(i).set(1, subSentence.get(i).get(1) + ":");
			} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(5).equals("case")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i + 1).get(5).equals("mark")) {
				subSentence.get(i + 1).set(1, ":" + subSentence.get(i + 1).get(1));
			} else if (subSentence.get(i).get(5).equals("nmod") && subSentence.get(i + 1).get(5).equals("root")) {
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
				try {
					if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("ADV")
							&& subSentence.get(i + 2).get(3).equals("ADV")) {
						subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + "("
								+ subSentence.get(i + 1).get(1) + " " + subSentence.get(i + 2).get(1) + "))");
						toDelete.add(i);
						toDelete.add(i + 1);
						i += 2;
					}
				} catch (Exception e1) {
				}

				if ((subSentence.get(i).get(1).contains("cel") || subSentence.get(i).get(1).contains("Cel")
						|| subSentence.get(i).get(1).contains("cea") || subSentence.get(i).get(1).contains("Cea"))
						&& subSentence.get(i + 1).get(1).contains("mai") && (subSentence.get(i + 2).get(3).equals("ADJ")
								|| subSentence.get(i + 2).get(3).equals("ADV"))) {
					subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + "(" + subSentence.get(i + 1).get(1)
							+ " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if ((subSentence.get(i).get(3).equals("ADV") || subSentence.get(i).get(3).equals("ADJ"))
						&& (subSentence.get(i + 1).get(3).equals("ADJ")
								|| subSentence.get(i + 1).get(3).equals("ADV"))) {

					subSentence.get(i + 1).set(1,
							"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
					toDelete.add(i);
					i += 1;
				} else if ((subSentence.get(i).get(3).equals("PROPN") || subSentence.get(i).get(5).equals("nsubj:pass")
						|| subSentence.get(i).get(5).equals("nsubj") || subSentence.get(i).get(5).equals("iobj"))
						&& subSentence.get(i + 1).get(3).equals("PROPN")) {
					subSentence.get(i).set(1,
							"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i).get(5).equals("nsubj")
						&& subSentence.get(i + 1).get(3).equals("ADJ")
						&& subSentence.get(i + 1).get(5).equals("amod")) {
					subSentence.get(i).set(1,
							"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				} /* pronumele posesive */
				else if (subSentence.get(i).get(3).equals("NOUN") && (subSentence.get(i + 1).get(1).contains("meu")
						|| subSentence.get(i + 1).get(1).contains("mea")
						|| subSentence.get(i + 1).get(1).contains("mei")
						|| subSentence.get(i + 1).get(1).contains("mele")
						|| subSentence.get(i + 1).get(1).contains("t??u")
						|| subSentence.get(i + 1).get(1).contains("ta")
						|| subSentence.get(i + 1).get(1).contains("t??i")
						|| subSentence.get(i + 1).get(1).contains("tale")
						|| subSentence.get(i + 1).get(1).contains("s??u")
						|| subSentence.get(i + 1).get(1).contains("sa")
						|| subSentence.get(i + 1).get(1).contains("s??i")
						|| subSentence.get(i + 1).get(1).contains("sale")
						|| subSentence.get(i + 1).get(1).contains("nostru")
						|| subSentence.get(i + 1).get(1).contains("noastr??")
						|| subSentence.get(i + 1).get(1).contains("no??tri")
						|| subSentence.get(i + 1).get(1).contains("noastre")
						|| subSentence.get(i + 1).get(1).contains("vostru")
						|| subSentence.get(i + 1).get(1).contains("voastr??")
						|| subSentence.get(i + 1).get(1).contains("vo??tri")
						|| subSentence.get(i + 1).get(1).contains("voastre")
						|| subSentence.get(i + 1).get(1).contains("lor")
						|| subSentence.get(i + 1).get(1).contains("lui"))) {

					subSentence.get(i).set(1,
							"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
					toDelete.add(i + 1);
					i += 1;
				}
			} catch (Exception e) {
			}
			i += 1;
		}

		int temp;
		while (toDelete.size() != 0) {
			temp = toDelete.get(toDelete.size() - 1);
			subSentence.remove(temp);
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
			
			try {
				if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(5).equals("mark")
						&& subSentence.get(i + 2).get(3).equals("PRON")
						&& subSentence.get(i + 3).get(3).equals("AUX")) {
					subSentence.get(i + 3).set(1, "(" + subSentence.get(i).get(1) + "((" + subSentence.get(i + 1).get(1)
							+ " " + subSentence.get(i + 2).get(1) + ")" + subSentence.get(i + 3).get(1) + "))");
					toDelete.add(i);
					toDelete.add(i + 1);
					toDelete.add(i + 2);
					i += 3;
				}
				if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("PART")
						&& subSentence.get(i + 2).get(3).equals("ADV")
						&& subSentence.get(i + 3).get(3).equals("VERB")) {
					subSentence.get(i + 3).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1)
							+ " (" + subSentence.get(i + 2).get(1) + " " + subSentence.get(i + 3).get(1) + ")))");
					toDelete.add(i);
					toDelete.add(i + 1);
					toDelete.add(i + 2);
					i += 3;
				}
				if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(5).equals("mark")
						&& subSentence.get(i + 2).get(5).equals("expl:poss")
						&& subSentence.get(i + 3).get(3).equals("VERB")) {
					subSentence.get(i + 3).set(1,
							"((" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1) + " "
									+ subSentence.get(i + 2).get(1) + ")) " + subSentence.get(i + 3).get(1) + ")");
					toDelete.add(i);
					toDelete.add(i + 1);
					toDelete.add(i + 2);
					i += 3;
				}
				if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(5).equals("expl:pass")
						&& subSentence.get(i + 2).get(3).equals("AUX")
						&& subSentence.get(i + 3).get(3).equals("VERB")) {
					subSentence.get(i + 3).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1)
							+ " (" + subSentence.get(i + 2).get(1) + " " + subSentence.get(i + 3).get(1) + ")))");
					toDelete.add(i);
					toDelete.add(i + 1);
					toDelete.add(i + 2);
					i += 3;
				} else if ((subSentence.get(i).get(3).equals("AUX") || subSentence.get(i).get(5).equals("expl")
						|| subSentence.get(i).get(5).equals("iobj") || subSentence.get(i).get(5).equals("expl:poss"))
						&& subSentence.get(i + 1).get(3).equals("AUX")
						&& subSentence.get(i + 2).get(3).equals("VERB")) {
					subSentence.get(i + 2).set(1, ":((" + subSentence.get(i).get(1) + " "
							+ subSentence.get(i + 1).get(1) + ") " + subSentence.get(i + 2).get(1) + ")");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(5).equals("iobj")
						&& subSentence.get(i + 2).get(3).equals("VERB")) {
					subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1)
							+ " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("obj")
						&& subSentence.get(i + 2).get(5).equals("root")) {
					subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1)
							+ " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if ((subSentence.get(i).get(5).equals("mark") || subSentence.get(i).get(5).equals("acl"))
						&& subSentence.get(i + 1).get(5).equals("mark")
						&& subSentence.get(i + 2).get(3).equals("VERB")) {
					subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1)
							+ " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if ((subSentence.get(i).get(5).equals("expl:pv") || subSentence.get(i).get(5).equals("obj")
						|| subSentence.get(i).get(5).equals("expl:pass")) && subSentence.get(i + 1).get(3).equals("AUX")
						&& subSentence.get(i + 2).get(3).equals("VERB")) {
					subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1)
							+ " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if (subSentence.get(i).get(3).equals("PART") && subSentence.get(i + 1).get(3).equals("AUX")
						&& subSentence.get(i + 2).get(3).equals("VERB")) {
					subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1)
							+ " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if (subSentence.get(i).get(3).equals("PART") && subSentence.get(i + 1).get(3).equals("AUX")
						&& subSentence.get(i + 2).get(3).equals("AUX")) {
					subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1)
							+ " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if ((subSentence.get(i).get(1).equals("s??") || subSentence.get(i).get(1).equals("a"))
						&& subSentence.get(i + 1).get(1).equals("se") && subSentence.get(i + 2).get(3).equals("VERB")) {
					subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1)
							+ " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if (subSentence.get(i).get(1).toLowerCase().equals("se")
						&& (subSentence.get(i + 1).get(1).equals("va") || subSentence.get(i + 1).get(1).equals("vor"))
						&& subSentence.get(i + 2).get(3).equals("VERB")) {
					subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1)
							+ " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if ((subSentence.get(i).get(1).equals("s??") || subSentence.get(i).get(1).equals("a"))
						&& subSentence.get(i + 1).get(1).equals("i") && subSentence.get(i + 2).get(1).equals("se")
						&& subSentence.get(i + 3).get(3).equals("VERB")) {
					subSentence.get(i + 3).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1)
							+ " (" + subSentence.get(i + 2).get(1) + " " + subSentence.get(i + 3).get(1) + ")))");
					toDelete.add(i);
					toDelete.add(i + 1);
					toDelete.add(i + 2);
					i += 3;
				} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(1).equals("se")
						&& subSentence.get(i + 2).get(3).equals("VERB")) {
					subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1)
							+ " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("PART")
						&& subSentence.get(i + 2).get(3).equals("VERB")) {
					subSentence.get(i + 2).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1)
							+ " " + subSentence.get(i + 2).get(1) + "))");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if ((subSentence.get(i).get(1).equals("se") || subSentence.get(i).get(1).equals("se:"))
						&& subSentence.get(i + 1).get(3).equals("VERB")
						&& subSentence.get(i + 2).get(3).equals("VERB")) {
					subSentence.get(i + 2).set(1, "((" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1)
							+ ") " + subSentence.get(i + 2).get(1) + ")");
					toDelete.add(i);
					toDelete.add(i + 1);
					i += 2;
				} else if (subSentence.get(i).get(3).equals("PRON") && subSentence.get(i + 1).get(1).equals("se")
						&& subSentence.get(i + 2).get(1).equals("mai")
						&& subSentence.get(i + 3).get(3).equals("VERB")) {
					subSentence.get(i + 3).set(1, "(" + subSentence.get(i).get(1) + " (" + subSentence.get(i + 1).get(1)
							+ " (" + subSentence.get(i + 2).get(1) + " " + subSentence.get(i + 3).get(1) + ")))");
					toDelete.add(i);
					toDelete.add(i + 1);
					toDelete.add(i + 2);
					i += 3;
				} else if ((subSentence.get(i).get(1).toLowerCase().equals("se")
						|| subSentence.get(i).get(1).equals("se:")) && subSentence.get(i + 1).get(3).equals("VERB")) {
					subSentence.get(i + 1).set(1,
							"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
					toDelete.add(i);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("PART") && subSentence.get(i + 1).get(3).equals("AUX")) {
					subSentence.get(i + 1).set(1,
							"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
					toDelete.add(i);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("AUX") && (subSentence.get(i + 1).get(3).equals("VERB")
						|| subSentence.get(i + 1).get(3).equals("AUX"))) {
					subSentence.get(i + 1).set(1,
							"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
					toDelete.add(i);
					i += 1;
				} else if ((subSentence.get(i).get(5).equals("iobj") || subSentence.get(i).get(5).equals("mark"))
						&& subSentence.get(i + 1).get(3).equals("VERB")) {
					subSentence.get(i + 1).set(1,
							"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
					toDelete.add(i);
					i += 1;
				} else if (subSentence.get(1).get(5).equals("expl") && subSentence.get(i + 1).get(5).equals("root")) {
					subSentence.get(i + 1).set(1,
							"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
					toDelete.add(i);
					i += 1;
				} else if (subSentence.get(i).get(1).contains("-") && (subSentence.get(i + 1).get(3).equals("AUX")
						|| subSentence.get(i + 1).get(3).equals("VERB"))) {
					subSentence.get(i + 1).set(1,
							"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
					toDelete.add(i);
					i += 1;
				} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("VERB")) {
					subSentence.get(i + 1).set(1,
							"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
					toDelete.add(i);
					i += 1;
				} else if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(3).equals("VERB")) {
					subSentence.get(i + 1).set(1,
							"(" + subSentence.get(i).get(1) + " " + subSentence.get(i + 1).get(1) + ")");
					toDelete.add(i);
					i += 1;
				}
			} catch (Exception e) {
			}
			i += 1;
		}

		int temp;
		
		while (toDelete.size() != 0) {
			temp = toDelete.get(toDelete.size() - 1);
			subSentence.remove(temp);
			toDelete.remove(toDelete.size() - 1);
		}

		toDelete.removeAll(toDelete);
		i = 0;

		while (i < subSentence.size()) {
			
			try {
				if (subSentence.get(i).get(1).toLowerCase().equals("nu")
						|| subSentence.get(i).get(1).toLowerCase().equals("n-")
						|| subSentence.get(i).get(1).toLowerCase().equals("nu:")
						|| subSentence.get(i).get(1).equals(":nu") || subSentence.get(i).get(1).equals(":nu:")) {
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
			temp = toDelete.get(toDelete.size() - 1);
			subSentence.remove(temp);
			toDelete.remove(toDelete.size() - 1);
		}

		return subSentence;
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
			subSentence.get(0).remove(5);
			subSentence.get(0).remove(4);
			subSentence.get(0).remove(3);
			subSentence.get(0).remove(2);
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
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("ADJ")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("DET")
				&& subSentence.get(i + 2).get(3).equals("NOUN")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("AUX") && subSentence.get(i + 1).get(3).equals("ADV")
				&& subSentence.get(i + 2).get(3).equals("ADJ")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("NOUN") && subSentence.get(i + 1).get(3).equals("PROPN")
				&& subSentence.get(i + 2).get(3).equals("ADJ")) {
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
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("ADP")
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
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(5).equals("nmod")
				&& subSentence.get(i + 2).get(5).equals("nmod")) {
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
				&& subSentence.get(i + 2).get(5).equals("nmod")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("AUX")
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
		} else if (subSentence.get(i).get(3).equals("CCONJ") && subSentence.get(i + 1).get(5).equals("nsubj")
				&& subSentence.get(i + 2).get(3).equals("ADV")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 2, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("ADV")
				&& subSentence.get(i + 2).get(3).equals("ADV")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 2, i);
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(5).equals("obj")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("VERB")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 2, i);
		} else if (subSentence.get(i).get(5).equals("mark") && subSentence.get(i + 1).get(5).equals("det")
				&& subSentence.get(i + 2).get(5).equals("nsubj")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(3).equals("AUX") && subSentence.get(i + 1).get(5).equals("det")
				&& subSentence.get(i + 2).get(5).equals("nsubj")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADV")) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(6).equals(subSentence.get(i + 1).get(6))
				&& subSentence.get(i + 1).get(0).equals(subSentence.get(i + 2).get(6))) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(6).equals(subSentence.get(i + 1).get(0))
				&& subSentence.get(i + 1).get(6).equals(subSentence.get(i + 2).get(0))) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(0).equals(subSentence.get(i + 2).get(6))
				&& subSentence.get(i + 1).get(6).equals(subSentence.get(i + 2).get(0))) {
			subSentenceTemp = this.combThree(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2), 1, i);
		} else if (subSentence.get(i).get(6).equals(subSentence.get(i + 1).get(0))
				&& subSentence.get(i + 1).get(6).equals(subSentence.get(i + 2).get(0))) {
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

		if (subSentence.get(i).get(5).equals("obj") && subSentence.get(i + 1).get(5).equals("root")
				&& subSentence.get(i + 2).get(5).equals("advmod") && subSentence.get(i + 3).get(5).equals("obl")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(3).equals("DET") && subSentence.get(i + 1).get(5).equals("nsubj")
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
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("PRON")
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
				&& subSentence.get(i + 2).get(5).equals("obj") && subSentence.get(i + 3).get(5).equals("nmod")) {
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
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
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
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("AUX")
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
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("ADJ")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
		} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("case")
				&& subSentence.get(i + 2).get(5).equals("obl") && subSentence.get(i + 3).get(5).equals("nmod")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(5).equals("advmod") && subSentence.get(i + 1).get(5).equals("case")
				&& subSentence.get(i + 2).get(5).equals("det") && subSentence.get(i + 3).get(5).equals("obl")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(5).equals("root") && subSentence.get(i + 1).get(5).equals("case")
				&& subSentence.get(i + 2).get(5).equals("obl") && subSentence.get(i + 3).get(5).equals("nummod")) {
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
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("AUX")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("PRON")
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
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
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
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
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
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 1, i);
		} else if (subSentence.get(i).get(3).equals("AUX") && subSentence.get(i + 1).get(3).equals("ADV")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("PRON")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("CCONJ")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("DET")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("NUM") && subSentence.get(i + 3).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADJ") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("ADV")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 3, i);
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
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("AUX")
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
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("ADV")) {
			subSentenceTemp = this.combFour(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), 2, i);
		} else if (subSentence.get(i).get(3).equals("ADP") && subSentence.get(i + 1).get(3).equals("ADV")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("PRON")) {
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
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
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
		} else if ((subSentence.get(i).get(3).equals("PRON") || subSentence.get(i).get(5).equals("nsubj"))
				&& subSentence.get(i + 1).get(3).equals("AUX") && subSentence.get(i + 2).get(3).equals("DET")
				&& subSentence.get(i + 3).get(3).equals("NOUN") && subSentence.get(i + 4).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(3).equals("ADV") && subSentence.get(i + 1).get(3).equals("PRON")
				&& subSentence.get(i + 2).get(3).equals("AUX") && subSentence.get(i + 3).get(3).equals("VERB")
				&& (subSentence.get(i + 4).get(5).equals("ADV") || subSentence.get(i + 4).get(5).equals("NOUN")
						|| subSentence.get(i + 4).get(5).equals("ADJ"))) {
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
				&& subSentence.get(i + 4).get(5).equals("nmod")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(3).equals("AUX") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("DET") && subSentence.get(i + 3).get(3).equals("NOUN")
				&& subSentence.get(i + 4).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 10, i);
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(5).equals("acl")
				&& subSentence.get(i + 2).get(5).equals("obj") && subSentence.get(i + 3).get(5).equals("case")
				&& subSentence.get(i + 4).get(5).equals("nmod")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 6, i);
		} else if (subSentence.get(i).get(3).equals("DET") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")
				&& subSentence.get(i + 4).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 5, i);
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("ADJ")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("ADP")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 6, i);
		} else if ((subSentence.get(i).get(3).equals("PRON") || subSentence.get(i).get(5).equals("nsubj"))
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
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADV") && subSentence.get(i + 3).get(3).equals("DET")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADP") && subSentence.get(i + 3).get(3).equals("NOUN")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
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
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("NOUN") && subSentence.get(i + 3).get(3).equals("DET")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("DET") && subSentence.get(i + 3).get(3).equals("NOUN")
				&& subSentence.get(i + 4).get(3).equals("ADJ")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 8, i);
		} else if (subSentence.get(i).get(3).equals("VERB") && subSentence.get(i + 1).get(3).equals("NOUN")
				&& subSentence.get(i + 2).get(3).equals("VERB") && subSentence.get(i + 3).get(3).equals("ADP")
				&& subSentence.get(i + 4).get(3).equals("ADV")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 6, i);
		} else if (subSentence.get(i).get(5).equals("nsubj") && subSentence.get(i + 1).get(3).equals("VERB")
				&& subSentence.get(i + 2).get(3).equals("ADV") && subSentence.get(i + 3).get(3).equals("VERB")
				&& subSentence.get(i + 4).get(3).equals("NOUN")) {
			subSentenceTemp = this.combFive(subSentence.get(i), subSentence.get(i + 1), subSentence.get(i + 2),
					subSentence.get(i + 3), subSentence.get(i + 4), 6, i);
		}

		return subSentenceTemp.size() > 0 ? subSentenceTemp : null;
	}

	private ArrayList<String> combFive(ArrayList<String> lst1, ArrayList<String> lst2, ArrayList<String> lst3,
			ArrayList<String> lst4, ArrayList<String> lst5, int comb, int i) {

		ArrayList<String> temp = new ArrayList<String>();

		temp.add(lst1.get(0));

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

		temp.add(String.valueOf(
				(Double.parseDouble(lst1.get(6)) + Double.parseDouble(lst2.get(6)) + Double.parseDouble(lst3.get(6))
						+ Double.parseDouble(lst4.get(6)) + Double.parseDouble(lst5.get(6))) / 5));

		return temp;
	}

	private ArrayList<String> combFour(ArrayList<String> lst1, ArrayList<String> lst2, ArrayList<String> lst3,
			ArrayList<String> lst4, int comb, int i) {

		ArrayList<String> temp = new ArrayList<String>();

		temp.add(lst1.get(0));

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

		temp.add(String.valueOf((Double.parseDouble(lst1.get(6)) + Double.parseDouble(lst2.get(6))
				+ Double.parseDouble(lst3.get(6)) + Double.parseDouble(lst4.get(6))) / 4));

		return temp;
	}

	private ArrayList<String> combThree(ArrayList<String> lst1, ArrayList<String> lst2, ArrayList<String> lst3,
			int comb, int i) {

		ArrayList<String> temp = new ArrayList<String>();

		temp.add(lst1.get(0));

		if (comb == 1) {
			temp.add("(" + lst1.get(1) + " (" + lst2.get(1) + " " + lst3.get(1) + "))");
		} else if (comb == 2) {
			temp.add("((" + lst1.get(1) + " " + lst2.get(1) + ") " + lst3.get(1) + ")");
		}

		temp.add(String.valueOf(
				(Double.parseDouble(lst1.get(6)) + Double.parseDouble(lst2.get(6)) + Double.parseDouble(lst3.get(6)))
						/ 3));

		return temp;
	}

	private ArrayList<String> combTwo(ArrayList<String> lst1, ArrayList<String> lst2) {

		ArrayList<String> temp = new ArrayList<String>();

		temp.add(lst1.get(0));

		temp.add("(" + lst1.get(1) + " " + lst2.get(1) + ")");

		temp.add(String.valueOf((Double.parseDouble(lst1.get(6)) + Double.parseDouble(lst2.get(6))) / 2));

		return temp;
	}

	public ArrayList<ArrayList<String>> setFinalBrackets(ArrayList<ArrayList<String>> subSentence) {

		ArrayList<Double> position = new ArrayList<Double>();

		for (int i = 0; i < subSentence.size(); i++) {
			position.add(
					((Double.parseDouble(subSentence.get(i).get(0)) + Double.parseDouble(subSentence.get(i).get(2)))
							/ 2));
		}
		Collections.sort(position);

		for (int i = 0; i < subSentence.size(); i++) {
			subSentence.get(i).set(2, String.valueOf(position.get(i)));

		}

		ArrayList<ArrayList<String>> subSentenceTemp = subSentence;
		ArrayList<String> subSentenceClosest = new ArrayList<String>();

		while (true) {

			try {
				double dependency = Double.parseDouble(subSentence.get(1).get(2));
				ArrayList<String> toDelete = subSentence.get(1);
				subSentenceTemp.remove(subSentence.get(1));

				ArrayList<String> dependencyClosest = this.closest(subSentenceTemp, dependency);
				ArrayList<String> dependencyToDelete = new ArrayList<String>(toDelete);
				double average = (Double.parseDouble(dependencyClosest.get(2))
						+ Double.parseDouble(dependencyToDelete.get(2))) / 2;

				if (Double.parseDouble(dependencyClosest.get(0)) < Double.parseDouble(dependencyToDelete.get(0))) {
					subSentenceClosest.add(String.valueOf((Double.parseDouble(dependencyClosest.get(0))
							+ Double.parseDouble(dependencyToDelete.get(0))) / 2));

					subSentenceClosest.add("(" + dependencyClosest.get(1) + " " + dependencyToDelete.get(1) + ")");
				} else {
					subSentenceClosest.add(String.valueOf((Double.parseDouble(dependencyClosest.get(0))
							+ Double.parseDouble(dependencyToDelete.get(0))) / 2));

					subSentenceClosest.add("(" + dependencyToDelete.get(1) + " " + dependencyClosest.get(1) + ")");
				}

				subSentenceClosest.add(String.valueOf(average));

				for (int i = 0; i < subSentenceTemp.size(); i++) {
					if (subSentenceTemp.get(i).get(0).equals(dependencyClosest.get(0))) {
						subSentenceTemp.set(i, new ArrayList<String>(subSentenceClosest));
					}
				}
				subSentenceClosest.removeAll(subSentenceClosest);
			} catch (Exception e) {
				break;
			}
		}

		return subSentenceTemp;
	}

	// returnarea liniei cu cea mai aproape dependenta de valoarea introdusa
	private ArrayList<String> closest(ArrayList<ArrayList<String>> list, double value) {
		
		int indexClosest = 0;
		double difference = Math.abs(Double.parseDouble(list.get(0).get(2)) - value);

		for (int i = 1; i < list.size(); i++) {
			double differenceTemp = Math.abs(Double.parseDouble(list.get(i).get(2)) - value);

			if (difference > differenceTemp) {
				indexClosest = i;
				difference = differenceTemp;
			}
		}

		return list.get(indexClosest);
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

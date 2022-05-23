package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.io.RuntimeIOException;
import edu.stanford.nlp.pipeline.CoNLLUReader;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;

public class SampleController {
	@FXML
	private TextArea inputText, outputText;
	@FXML
	private ComboBox<String> library;

	@FXML
	public void initialize() {
		library.getItems().add("STANZA");
		library.getItems().add("SPACY");
		library.getSelectionModel().selectFirst();
	}

	public void inputBtn() throws IOException {
		
		outputText.setText("");
		
		String text = PunctilogSentence.initialProcessing(inputText.getText());
		
		File conllu = new File("ConlluSetence.conllu");
		if (conllu.exists()) {
			conllu.delete();
		}
		
		for (String input : text.split("\\. ")) {
			// se inlocuieste " cu caracterul | pentru a se transmite programului python
			input = input.replaceAll("\"", "|");
			
			ProcessBuilder builder = new ProcessBuilder("python",
					System.getProperty("user.dir") + "\\CreateConlluFile.py", input,
					String.valueOf(library.getSelectionModel().getSelectedIndex() + 1));
			
			Process process = builder.start();
			
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader errors = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			String lines = null;
			
			while ((lines = reader.readLine()) != null) {
				System.out.println(lines);
			}
			
			if ((lines = errors.readLine()) != null) {
				do {
					System.out.println(lines);
				} while ((lines = errors.readLine()) != null);
			}

			ArrayList<ArrayList<String>> subSentence = new ArrayList<ArrayList<String>>();

			try {
				CoNLLUReader conllUReader = new CoNLLUReader();
				
				List<CoNLLUReader.CoNLLUDocument> docs = conllUReader
						.readCoNLLUFileCreateCoNLLUDocuments("ConlluSentence.conllu");

				for (CoNLLUReader.CoNLLUDocument doc : docs) {
					for (CoNLLUReader.CoNLLUSentence conllSent : doc.sentences) {

						PunctilogSentence sentence = new PunctilogSentence(conllSent);

						subSentence = sentence.isExpression();
						
						subSentence = sentence.isDirectSpeech();
						
						subSentence = sentence.NER();
						
						subSentence = sentence.isModifier(subSentence);
						
						subSentence = sentence.isCalificator(subSentence);

						subSentence = sentence.isVerbPart(subSentence);
						
						subSentence = sentence.isConjunction(subSentence);

						
						ArrayList<ArrayList<String>> subSentences = new ArrayList<ArrayList<String>>();
						ArrayList<ArrayList<ArrayList<String>>> mainSentence = new ArrayList<ArrayList<ArrayList<String>>>();
						ArrayList<ArrayList<ArrayList<String>>> subMainSentence = new ArrayList<ArrayList<ArrayList<String>>>();

						for (int i = 0; i < subSentence.size(); i++) {

							if (subSentence.get(i).get(1).equals(",")) {
								mainSentence.add(new ArrayList<ArrayList<String>>(subSentences));
								subSentences.removeAll(subSentences);
							} else {
								subSentences.add(subSentence.get(i));
							}
						}

						mainSentence.add(new ArrayList<ArrayList<String>>(subSentences));
						subSentences.removeAll(subSentences);

						for (ArrayList<ArrayList<String>> subSentenceTemp : mainSentence) {

							subSentence = subSentenceTemp;

							subMainSentence = sentence.divideSentence(subSentence);
							
							
							for (int i = 0; i < subMainSentence.size(); i++) {
								subMainSentence.set(i, sentence.addBrackets(subMainSentence.get(i)));
							}
							
							
							
							for (int i = 0; i < subMainSentence.size(); i++) {
								subMainSentence.set(i, sentence.setFinalBrackets(subMainSentence.get(i)));
							}

							ArrayList<ArrayList<ArrayList<String>>> temp = new ArrayList<ArrayList<ArrayList<String>>>(
									subMainSentence);
							subMainSentence.removeAll(subMainSentence);
							for (int i = temp.size() - 1; i >= 0; i--) {
								subMainSentence.add(temp.get(i));
							}

							
							subSentence = sentence.addBrackets(subSentence);

							subSentence = sentence.setFinalBrackets(subSentence);

							// adaugarea virgulei intre constituienti
							subSentence.get(0).set(1, subSentence.get(0).get(1) + ",");

							ArrayList<ArrayList<String>> subList = new ArrayList<ArrayList<String>>();
							for (ArrayList<String> item : subSentence) {
								subList.add(item);
							}
							subSentence = sentence.addBrackets(subList);

							while (subSentence.get(0).get(1).contains("(:")) {
								subSentence.get(0).set(1, subSentence.get(0).get(1).replace("(:", ":("));
							}
							while (subSentence.get(0).get(1).contains(":)")) {
								subSentence.get(0).set(1, subSentence.get(0).get(1).replace(":)", "):"));
							}
							while (subSentence.get(0).get(1).contains(",)")) {
								subSentence.get(0).set(1, subSentence.get(0).get(1).replace(",)", "),"));
							}
							while (subSentence.get(0).get(1).contains("::")) {
								subSentence.get(0).set(1, subSentence.get(0).get(1).replace("::", ":"));
							}

							if (subSentence.get(0).get(1).charAt(subSentence.get(0).get(1).length() - 1) == ',') {
								subSentence.get(0).set(1,
										subSentence.get(0).get(1).substring(0, subSentence.get(0).get(1).length() - 1));
							}
							outputText.setText(outputText.getText() + subSentence.get(0).get(1) + "\n");
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeIOException(e);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	public void clearBtn() {
		inputText.setText("");
		outputText.setText("");
	}

}

package application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.io.RuntimeIOException;
import edu.stanford.nlp.pipeline.CoNLLUReader;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class SampleController {
	@FXML
	private TextArea inputText, outputText;
	
	public void inputBtn() {
		
		outputText.setText("");
		
		ArrayList<ArrayList<String>> subSentence = new ArrayList<ArrayList<String>>();;
		
		try {
			CoNLLUReader conllUReader = new CoNLLUReader();
			List<CoNLLUReader.CoNLLUDocument> docs = conllUReader
					.readCoNLLUFileCreateCoNLLUDocuments(inputText.getText());
			
			for (CoNLLUReader.CoNLLUDocument doc : docs) {
				for (CoNLLUReader.CoNLLUSentence conllSent : doc.sentences) {
					
					PunctilogSentence sentence = new PunctilogSentence(conllSent);
					
					sentence.isExpression();
					
					ArrayList<ArrayList<String>> subList = new ArrayList<ArrayList<String>>();
					ArrayList<ArrayList<ArrayList<String>>> mainList = new ArrayList<ArrayList<ArrayList<String>>>();
					
					subSentence = new ArrayList<ArrayList<String>>();
					for (int i = 0; i <sentence.getLengthSentencePunctilog(); i++) {
						subList.add(sentence.getSentencePunctilogLine(i));
					}
					
					subSentence = sentence.isDirectSpeech(subList);
					
					subList.clear();
					
					for (int i = 0; i < sentence.getLengthSentencePunctilog(); i++) {

						if (sentence.getSentencePunctilogField(i, 1).equals(",")) {
							mainList.add(new ArrayList<ArrayList<String>>(subList));
							subList.clear();
						} else {
							subList.add(sentence.getSentencePunctilogLine(i));
						}
					}

					mainList.add(new ArrayList<ArrayList<String>>(subList));
					subList.clear();
					

					
					
					for (ArrayList<ArrayList<String>> mainListTemp : mainList) {

						subSentence = sentence.isModifier(mainListTemp);

						subSentence = sentence.isCalificator(subSentence);
						
						subSentence = sentence.isVerbPart(subSentence);
						
						subSentence = sentence.addBrackets(subSentence);
						
						subSentence = sentence.setFinalBrackets(subSentence);


						subSentence.get(0).set(1, subSentence.get(0).get(1) + ",");
						
						for (int i = 0; i < subSentence.size(); i++) {
							subList.add(subSentence.get(i));
						}
					}

					subSentence = sentence.setFinalBrackets(subList);
					
					while (true) {
						if (subSentence.get(0).get(1).contains("(:")) {
							subSentence.get(0).set(1, subSentence.get(0).get(1).replace("(:", ":("));
						} else {
							break;
						}
					}
					
					while (true) {
						if (subSentence.get(0).get(1).contains(":)")) {
							subSentence.get(0).set(1, subSentence.get(0).get(1).replace(":)", "):"));
						} else {
							break;
						}
					}
					
					while (true) {
						if (subSentence.get(0).get(1).contains(",)")) {
							subSentence.get(0).set(1, subSentence.get(0).get(1).replace(",)", "),"));
						} else {
							break;
						}
					}
					
					if (subSentence.get(0).get(1).charAt(subSentence.get(0).get(1).length()-1) == ',') {
						subSentence.get(0).set(1, subSentence.get(0).get(1).substring(0, subSentence.get(0).get(1).length()-1));
					}
					
					outputText.setText(outputText.getText() + subSentence.get(0).get(1) + "\n\n");
				}
			}
			
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public void clearBtn() {
		inputText.setText("");
		outputText.setText("");
	}

}

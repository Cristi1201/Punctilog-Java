import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

public class SomeJavaProgram {

	public static void main(String a[]) throws IOException {
		
//		String sentence;
//		Scanner sc = new Scanner(System.in);
//		System.out.println("Sentence :");
//		sentence = sc.nextLine();
//		
//		Process process = Runtime.getRuntime().exec("python SomePythonProgram.py " + sentence);
//		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//		BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//		
//		System.out.println(reader.readLine());
//		while (String.valueOf(error.readLine()) != null) {
//			System.out.println(error.readLine());
//		}
//		reader.close();
//		
//		sc.close();
		
		
		
		
		
		
		
//		ProcessBuilder builder = new ProcessBuilder("python", System.getProperty("user.dir") + "\\SomePythonProgram.py", "1", "4");
//		Process process = builder.start();
//		
//		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//		BufferedReader readers = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//		
//		String lines = null;
//		
//		while ((lines=reader.readLine()) != null) {
//			System.out.println(lines);
//		}
//		
//		while ((lines=readers.readLine()) != null) {
//			System.out.println(lines);
//		}
		
		
		
		Scanner sc = new Scanner(System.in);
		System.out.println("Sentence : ");
		String sentence = sc.nextLine();
		
		ProcessBuilder builder = new ProcessBuilder("python", System.getProperty("user.dir") + "\\SomePythonProgram.py", sentence);
		Process process = builder.start();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		BufferedReader readers = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		
		String lines = null;
		
		while ((lines=reader.readLine()) != null) {
			System.out.println(lines);
		}
		
		while ((lines=readers.readLine()) != null) {
			System.out.println(lines);
		}
		
		sc.close();
		
		
	}
}

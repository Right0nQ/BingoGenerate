package bingo;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class Main extends Application {
	private static final int BOARD_SIZE = 5;
	private static final int MAX_SEED_LENGTH = 10;

	static String color = "#ffaaaa";
	static String darkColor = darken(color);

	private static final String HEX_COLOR = "#[0123456789abcdefABCDEF]{6}";

	private static final String[] ON_STYLES = { "-fx-font-size: 11px;-fx-border-color: Gray;-fx-background-color: " + color,
			"-fx-font-size: 11px;-fx-border-color: Gray;-fx-background-color: " + darkColor };
	private static final String[] OFF_STYLES = { "-fx-font-size: 11px;-fx-border-color: Gray;-fx-background-color: #eeeeee",
			"-fx-font-size: 11px;-fx-border-color: Gray;-fx-background-color: #dddddd" };

	byte[][] boardState;// 0th bit is on/off, 1st bit is mouse in/out
	Node win;

	public static void main(String[] args) {
		launch(args);
	}

	private static String getStyle(byte b) {
		if ((b & 1) == 1) {
			return ON_STYLES[(b >> 1) & 1];
		} else {
			return OFF_STYLES[(b >> 1) & 1];
		}
	}

	private static final double DARK = 0.9;

	private static String darken(String color) {
		int col = Integer.parseInt(color.substring(1), 16);
		byte[] darker = { (byte) ((255 & (col >> 16)) * DARK), (byte) ((255 & (col >> 8)) * DARK),
				(byte) ((255 & col) * DARK) };

		return "#" + byteString(darker);
	}

	private static String byteString(byte[] bytes) {
		StringBuilder ret = new StringBuilder();
		for (byte b : bytes) {
			ret.append(String.format("%02X", b));
		}
		return ret.toString();
	}

	private void recolor(GridPane board) {
		for (Node n : board.getChildren()) {
			String[] pos = n.getId().split(",");
			int posx = Integer.parseInt(pos[0]);
			int posy = Integer.parseInt(pos[1]);

			n.setStyle(getStyle(boardState[posy][posx]));
		}
	}

	private Node makeSquare(String text, int x, int y) {
		Button b = new Button(text);
		b.setMinSize(100, 100);
		b.setMaxSize(100, 100);
		b.setId(x + "," + y);
		b.setStyle(OFF_STYLES[0]);
		b.setWrapText(true);
		b.setTextAlignment(TextAlignment.CENTER);

		b.setOnMouseClicked(e -> {
			String[] pos = b.getId().split(",");
			int posx = Integer.parseInt(pos[0]);
			int posy = Integer.parseInt(pos[1]);

			boardState[posy][posx] = (byte) (1 ^ boardState[posy][posx]);

			b.setStyle(getStyle(boardState[posy][posx]));

			win.setVisible(bingoed());
		});

		b.setOnMouseEntered(e -> {
			String[] pos = b.getId().split(",");
			int posx = Integer.parseInt(pos[0]);
			int posy = Integer.parseInt(pos[1]);
			boardState[posy][posx] = (byte) (boardState[posy][posx] ^ 2);
			b.setStyle(getStyle(boardState[posy][posx]));
		});

		b.setOnMouseExited(e -> {
			String[] pos = b.getId().split(",");
			int posx = Integer.parseInt(pos[0]);
			int posy = Integer.parseInt(pos[1]);
			boardState[posy][posx] = (byte) (boardState[posy][posx] ^ 2);
			b.setStyle(getStyle(boardState[posy][posx]));
		});

		return b;
	}

	@Override
	public void start(Stage stage) throws Exception {

		ArrayList<ArrayList<String>> strs = new ArrayList<>();
		ArrayList<Integer> maxEntries = new ArrayList<>();
		Random rnd;

		List<String> params = getParameters().getUnnamed();
		String file = "";

		try {
			if (params.size() == 0)
				throw new Exception("");

			for (int i = 0; i < params.size(); i++) {
				file = params.get(i);
				BufferedReader read = new BufferedReader(new FileReader(file));

				ArrayList<String> list = new ArrayList<>();

				String curr = read.readLine();
				while (curr != null) {
					if (file.contains(".csv")) {
						Collections.addAll(list, curr.split(","));
					} else {
						list.add(curr);
					}
					curr = read.readLine();
				}

				read.close();

				strs.add(list);

				if (i < params.size() - 1 && params.get(i + 1).matches("-?\\d+")) {
					i++;
					maxEntries.add(Integer.parseInt(params.get(i)));
				} else {
					maxEntries.add(-1);
				}
			}

		} catch (FileNotFoundException e) {
			System.out.println("File " + file + " not found");
			stop();
		} catch (Exception e) {
			//e.printStackTrace();
			System.out.println(
					"Usage: java -jar <program>.jar <input_file_1> (<max_entries_1>) <input_file_2> (<max_entries_2>)...");
			stop();
		}

		rnd = new Random();

		int sum = 0;
		for (List<String> list : strs)
			sum += list.size();

		if (sum < BOARD_SIZE * BOARD_SIZE) {
			System.out.println(
					"Not enough entries. Requires " + (BOARD_SIZE * BOARD_SIZE) + ", input "
					+ strs.size() + ".");
			stop();
		}

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("bingo_layout.fxml"));
		Parent root = fxmlLoader.load();

		GridPane board = (GridPane) root.lookup("#board");

		fillBoard(strs, maxEntries, board, rnd);

		Button generate = (Button) root.lookup("#generate");
		TextField seed = (TextField) root.lookup("#random_seed");

		generate.setOnMouseClicked(e -> {
			String s = seed.getText();
			try {
				int n = Integer.parseInt(s);
				Random rnd2 = new Random(n);
				fillBoard(strs, maxEntries, board, rnd2);
			} catch (Exception ignored) {
			}
		});
		seed.setOnKeyTyped(e -> {
			int cursor = seed.getCaretPosition();

			if (seed.getText().length() > MAX_SEED_LENGTH) {
				seed.setText(seed.getText().substring(0, MAX_SEED_LENGTH));

				seed.positionCaret(cursor);
			}
		});

		Button changeColor = (Button) root.lookup("#change_color");
		TextField newColor = (TextField) root.lookup("#color");
		newColor.setText(color);

		changeColor.setOnMouseClicked(e -> {
			String str = newColor.getText();

			if (!Pattern.matches(HEX_COLOR, str))
				return;

			color = str;

			ON_STYLES[0] = ON_STYLES[0].replaceAll(HEX_COLOR, str);
			ON_STYLES[1] = ON_STYLES[1].replaceAll(HEX_COLOR, darken(str));

			recolor(board);

		});

		newColor.setOnKeyTyped(e -> {
			int cursor = newColor.getCaretPosition();

			if (newColor.getText().length() > 7) {
				newColor.setText(newColor.getText().substring(0, 7));

				newColor.positionCaret(cursor);
			}
		});

		win = root.lookup("#win");
		win.setVisible(false);

		// Group root = new Group(stack);

		Scene scene = new Scene(root, 500, 520);

		stage.setTitle("Bingo");
		stage.setScene(scene);
		stage.setResizable(false);
		stage.show();
	}

	private boolean bingoed() {
		f1: for (int x = 0; x < BOARD_SIZE; x++) {
			for (int y = 0; y < BOARD_SIZE; y++) {
				if ((boardState[y][x] & 1) != 1)
					continue f1;
			}
			return true;
		}

		f1: for (int y = 0; y < BOARD_SIZE; y++) {
			for (int x = 0; x < BOARD_SIZE; x++) {
				if ((boardState[y][x] & 1) != 1)
					continue f1;
			}
			return true;
		}

		for (int d = 0; d < BOARD_SIZE; d++) {
			if ((boardState[d][d] & 1) != 1)
				break;

			if (d == BOARD_SIZE - 1)
				return true;
		}

		for (int d = 0; d < BOARD_SIZE; d++) {
			if ((boardState[d][BOARD_SIZE - d - 1] & 1) != 1)
				break;

			if (d == BOARD_SIZE - 1)
				return true;
		}

		return false;

	}

	private void fillBoard(ArrayList<ArrayList<String>> strs, List<Integer> maxEntries, GridPane board, Random rnd) {
		boardState = new byte[BOARD_SIZE][BOARD_SIZE];

		board.getChildren().clear();

		ArrayList<ArrayList<String>> holdStrs = new ArrayList<>();

		for (ArrayList<String> ls : strs) {
			ArrayList<String> hold = new ArrayList<>(ls);
			holdStrs.add(hold);
		}

		ArrayList<String> toAdd = new ArrayList<>();
		int[] numEntries = new int[maxEntries.size()];

		for (int i = 0; i < BOARD_SIZE * BOARD_SIZE - 1; i++) {
			int list = rnd.nextInt(strs.size());
			int testList = list;
			while (numEntries[testList] >= maxEntries.get(list) && maxEntries.get(list) != -1) {
				list = Math.abs(list + 1) % numEntries.length;

				if (testList == list)
					break;
			}

			toAdd.add(holdStrs.get(list).remove(rnd.nextInt(holdStrs.get(list).size())));
			numEntries[list]++;
		}


		for (int x = 0; x < BOARD_SIZE; x++) {
			for (int y = 0; y < BOARD_SIZE; y++) {
				if (x == BOARD_SIZE / 2 && y == BOARD_SIZE / 2)
					board.add(makeSquare("FREE", x, y), x, y);
				else
					board.add(makeSquare(toAdd.remove(rnd.nextInt(toAdd.size())), x, y), x, y);
			}
		}
	}
}

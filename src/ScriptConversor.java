import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Script UDH
 * 
 * @author Thiago <tsprates@hotmail.com>
 */
public class ScriptConversor implements Runnable {

	private File inputfile;
	private File outputfile;
	private FileInputStream configFile;
	private Properties props = new Properties();

	private Long tempoInicio, tempoFim;

	private String delimeter;
	private String text_delimeter;

	private String inputfile_col_lat, inputfile_col_lng;
	private String outputfile_cols;

	private BufferedReader arqEntrada;
	private PrintStream arqSaida;

	// xml
	private DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	private DocumentBuilder db;
	private XPathFactory xpathFactory = XPathFactory.newInstance();

	public static void main(String[] args) {
		System.out.println(" Projeto - Atlas Brasil");
		System.out.println(" [SCRIPT] iniciando:");

		if (args.length > 0) {
			try {
				new ScriptConversor(args[0]);
			} catch (FileNotFoundException e) {
				System.err.println(" [Error] - Necessario arquivo de Config.");
				mostraAjuda();
			}
		} else {
			System.err.println(" [Error] - Necessario arquivo de Config.");
			mostraAjuda();
		}
	}

	private static void mostraAjuda() {
		System.out.println(" Exemplo de arquivo de configuracao:                    ");
		System.out.println(" *config.properties                                     ");
		System.out.println("														");
		System.out.println("inputfile=/caminho/para/arquivo/entrada.csv             ");
		System.out.println("outputfile=/caminho/para/arquivo/saida.csv              ");
		System.out.println("delimiter=,                                             ");
		System.out.println("text_delimiter=\"                                       ");
		System.out.println("inputfile_col_lat=lat                                   ");
		System.out.println("inputfile_col_lng=lng                                   ");
		System.out.println("geoservice=http://api.geo.org/find?lat=%s&lng=%s        ");
		System.out.println("outputfile_cols=A,B,C,D                                 ");
	}

	/**
	 * Define arquivo de entrada para leitura dos dados e arquivo de saida para
	 * os dados formatados.
	 * 
	 * @param pathFileConfig
	 *            Caminho do arquivo de entrada.
	 */
	public ScriptConversor(String pathFileConfig) throws FileNotFoundException {

		try {
			configFile = new FileInputStream(pathFileConfig);
			props.load(configFile);
		} catch (IOException e) {
			throw new RuntimeException("Erro ao carregar de propriedades do arquivo de config.");
		}

		setaPropsCsv(props);

		System.out.println(" Processando:");

		carregaThread();

		carregaLeitorXml();
	}

	/**
	 * 
	 */
	private void carregaThread() {
		if (inputfile.exists()) {
			new Thread(this).start();
		} else {
			throw new RuntimeException("[Erro] Arquivos inválidos.");
		}
	}

	/**
	 * 
	 */
	private void carregaLeitorXml() {
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("[Erro] Leitor de xml.");
		}
	}

	/**
	 * 
	 * @param props
	 */
	private void setaPropsCsv(Properties props) {
		inputfile = new File(props.getProperty("inputfile"));
		outputfile = new File(props.getProperty("outputfile"));

		delimeter = props.getProperty("delimiter");
		text_delimeter = props.getProperty("text_delimiter");

		inputfile_col_lat = props.getProperty("inputfile_col_lat");
		inputfile_col_lng = props.getProperty("inputfile_col_lng");

		outputfile_cols = props.getProperty("outputfile_cols");
	}

	@Override
	public void run() {
		try {
			arqEntrada = new BufferedReader(new FileReader(inputfile));
			arqSaida = new PrintStream(outputfile);

			StringBuilder linSaida = new StringBuilder();
			String linAtual;

			String arrColsConf[] = outputfile_cols.split(delimeter);

			// mapeamento das colunas
			Map<String, Integer> indexColunas = new HashMap<String, Integer>();

			String colProcurada, colAtual;
			
			String respServidor;
			String urlGeoservice;
			
			int linhas = 0;

			boolean existeColunas = false;
			int indexColLat, indexColLng;

			tempoInicio = System.currentTimeMillis();

			while ((linAtual = arqEntrada.readLine()) != null) {
				String colsLinhaAtual[] = linAtual.split(delimeter);
				linhas++;

				linSaida = new StringBuilder("");

				if (existeColunas == false) {
					for (int i = 0; i < colsLinhaAtual.length; i++) {
						indexColunas.put(colsLinhaAtual[i], i);
					}

					arqSaida.println(outputfile_cols);

					existeColunas = true;
					continue;
				}

				if (existeColunas == false) {
					throw new RuntimeException(
							"Não foi possivel identificar as colunas do arquivo.");
				}

				if (indexColunas.get(inputfile_col_lat) == null
						|| indexColunas.get(inputfile_col_lng) == null) {
					throw new RuntimeException(
							"Erro ao especificar colunas de latitude e longitude do arquivo de configuracao, "
									+ "possivelmente problema em definer o 'delimeter'.");
				}

				indexColLat = indexColunas.get(inputfile_col_lat);
				indexColLng = indexColunas.get(inputfile_col_lng);

				urlGeoservice = String.format(Locale.ENGLISH,
						props.getProperty("geoservice"), ""
								+ colsLinhaAtual[indexColLat], ""
								+ colsLinhaAtual[indexColLng]);

				respServidor = "";
				for (int i = 0; i < arrColsConf.length; i++) {
					if (!indexColunas.containsKey(arrColsConf[i])) {
						respServidor = getXmlDaUrl(urlGeoservice);
						break;
					}
				}

				for (int i = 0; i < arrColsConf.length; i++) {
					colProcurada = arrColsConf[i].trim();

					if (indexColunas.get(colProcurada) != null) {
						colAtual = colsLinhaAtual[indexColunas
								.get(colProcurada)];
					} else {
						colAtual = retornaTag(respServidor,
								colProcurada);
					}

					if (colAtual.startsWith(text_delimeter)) {
						colAtual += text_delimeter;
					} else {
						colAtual = text_delimeter + colAtual + text_delimeter;
					}

					linSaida.append(colAtual + delimeter);
				}

				System.out.println(linhas + ". " + urlGeoservice);

				arqSaida.println(retiraUltimoDelimiter(linSaida.toString()));

				esperaThread(linhas);
			}

			arqEntrada.close();
			arqSaida.close();

		} catch (IOException e) {
			System.err.println("Arquivo não existe.");
			e.printStackTrace();
		}

		tempoFim = System.currentTimeMillis();
		System.out.println("Fim. " + ((tempoFim - tempoInicio) / 1000L) + " segs");
	}

	/**
	 * Sleep thread.
	 * 
	 * @param linhas
	 */
	private void esperaThread(int linhas) {
		if ((linhas % 50) == 0) {
			try {
				Thread.sleep(5000L);
			} catch (InterruptedException e) {
			}
		}

		if ((linhas % 500) == 0) {
			try {
				Thread.sleep(10000L);
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * Ler o conteúdo XML devolvido pela URL.
	 * 
	 * @param url
	 *            Url do geoservice.
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private String getXmlDaUrl(String url) {
		InputStream is;
		BufferedReader br;
		String linha;

		try {
			is = new URL(url).openStream();
			br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			StringBuilder sb = new StringBuilder();

			while ((linha = br.readLine()) != null) {
				sb.append(linha);
			}

			br.close();
			return sb.toString();
		} catch (IOException e) {
			return "";
		}

	}

	/**
	 * Filtra tag procurada no XML devolvido pelo geoservice.
	 * 
	 * @param xml
	 *            String que representa uma XML de resposta de um webservice
	 * @param tag
	 *            Tag que procura-se encontrar no XML.
	 * @return Valor da tag encontrado.
	 */
	private String retornaTag(String xml, String tag) {
		try {
			Document document = db
					.parse(new InputSource(new StringReader(xml)));
			XPath xpath = xpathFactory.newXPath();
			return xpath.evaluate(tag, document);
		} catch (SAXException | IOException | XPathExpressionException e) {
			return "";
		}
	}

	/**
	 * Formata para retirar o último delimiter utilizado para o arquivo de
	 * entrada.
	 * 
	 * @param s
	 * @return String
	 */
	private String retiraUltimoDelimiter(String s) {
		return s.substring(0, s.length()
				- props.getProperty("delimiter").length());
	}

}

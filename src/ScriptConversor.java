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
 * Script para conversão de arquivos. UDH
 * 
 * @author Thiago <tsprates@hotmail.com>
 */
public class ScriptConversor implements Runnable {

	private File inputfile;
	private File outputfile;
	private FileInputStream configFile;
	private Long inicio, fim;

	// classe de propriedades de configuração
	private Properties props = new Properties();

	// delimeter - elemento que divide as colunas do csv
	private String delimeter;
	private String text_delimeter;

	// coluna lat e lng para compor url da geoservice
	private String inputfile_col_lat, inputfile_col_lng;
	private String outputfile_cols;

	private BufferedReader leitorArqEntrada;
	private PrintStream escritorArqSaida;

	// parseador de xml
	private DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	private DocumentBuilder db;
	private XPathFactory xpathFactory = XPathFactory.newInstance();

	// Script
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

	/**
	 * Criação de arquivo de configuração.
	 * 
	 */
	private static void mostraAjuda() {
		System.out
				.println(" Exemplo de arquivo de configuracao:                    ");
		System.out
				.println(" *config.properties                                     ");
		System.out
				.println("========================================================");
		System.out.println("														");
		System.out
				.println("inputfile=/caminho/para/arquivo/entrada.csv             ");
		System.out
				.println("outputfile=/caminho/para/arquivo/saida.csv              ");
		System.out
				.println("delimiter=,                                             ");
		System.out
				.println("text_delimiter=\"                                       ");
		System.out
				.println("inputfile_col_lat=lat                                   ");
		System.out
				.println("inputfile_col_lng=lng                                   ");
		System.out
				.println("geoservice=http://api.geo.org/find?lat=%s&lng=%s        ");
		System.out
				.println("outputfile_cols=A,B,C,D                                 ");
	}

	/**
	 * Define arquivo de entrada para leitura dos dados e arquivo de saída para
	 * os dados formatados.
	 * 
	 * @param pathFileConfig
	 *            Caminho do arquivo de entrada.
	 */
	public ScriptConversor(String pathFileConfig) throws FileNotFoundException {

		try {
			// carrega propriedades
			configFile = new FileInputStream(pathFileConfig);
			props.load(configFile);
		} catch (IOException e) {
			throw new RuntimeException("[Erro] carregar de propriedades do arquivo de config.");
		}

		inputfile = new File(props.getProperty("inputfile"));
		outputfile = new File(props.getProperty("outputfile"));

		// delimeter
		delimeter = props.getProperty("delimiter");
		text_delimeter = props.getProperty("text_delimiter");

		// nomes colunas lat e lng da config
		inputfile_col_lat = props.getProperty("inputfile_col_lat");
		inputfile_col_lng = props.getProperty("inputfile_col_lng");

		// colunas de saída
		outputfile_cols = props.getProperty("outputfile_cols");

		// inicio processo
		System.out.println(" Processando:");

		if (inputfile.exists()) {
			new Thread(this).start();
		} else {
			throw new RuntimeException("[Erro] Arquivos inválidos.");
		}

		// testa parseador de xml
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("[Erro] Leitor de xml.");
		}
	}

	@Override
	public void run() {
		try {
			leitorArqEntrada = new BufferedReader(new FileReader(inputfile));
			escritorArqSaida = new PrintStream(outputfile);

			StringBuilder linhaSaida = new StringBuilder();
			String linhaAtual;

			// colunas (propriedades) requeridas pelo cliente no arquivo de
			// configuração
			String arrColunasConfig[] = outputfile_cols.split(delimeter);

			// mapeamento das colunas
			Map<String, Integer> indiceColunas = new HashMap<String, Integer>();

			String colunaProcurada, colunaAtual;
			String respostaDoServidor;

			String urlDoGeoService;
			int numLinhasLidas = 0; // desconsidera linha de cabeçalho

			boolean jaExisteNomesDasColunas = false;
			int indiceColunaLat, indiceColunaLng;

			// tempo demorado no processo
			inicio = System.currentTimeMillis();

			while ((linhaAtual = leitorArqEntrada.readLine()) != null) {
				String colunasDaLinhaAtual[] = linhaAtual.split(delimeter);

				// quantidade de linhas lidas do arquivo
				numLinhasLidas++;

				// reseta linha de saida para novo registro
				linhaSaida = new StringBuilder("");

				// para descobrir cabeçalho
				if (jaExisteNomesDasColunas == false) {
					// faz mapeamento de colunas do arquivo de entrada
					for (int i = 0; i < colunasDaLinhaAtual.length; i++) {
						indiceColunas.put(colunasDaLinhaAtual[i], i);
					}

					// escreve cabeçalho arquivo de saída requerido para pesquisa
					escritorArqSaida.println(outputfile_cols);

					// sabe o nome das colunas do arquivo,
					// previne não fazer novamente o mapeamento
					jaExisteNomesDasColunas = true;
					continue;
				}

				// nomes das colunas necessários para identificar colunas
				// necessárias no arquivo de configurações
				if (jaExisteNomesDasColunas == false) {
					throw new RuntimeException(
							"[Erro] Nao foi possivel identificar as colunas do arquivo.");
				}

				if (indiceColunas.get(inputfile_col_lat) == null
						|| indiceColunas.get(inputfile_col_lng) == null) {
					throw new RuntimeException(
							"[Erro] Erro ao especificar colunas de latitude e longitude do arquivo de configuracao, "
									+ "possivelmente problema em definer o 'delimeter'.");
				}

				// posicão das colunas: latitude e longitude no arquivo de entrada
				indiceColunaLat = indiceColunas.get(inputfile_col_lat);
				indiceColunaLng = indiceColunas.get(inputfile_col_lng);

				//  url de requisição do geoservice com a latitude e longitude atual
				urlDoGeoService = String.format(Locale.ENGLISH,
						props.getProperty("geoservice"), 
							"" + colunasDaLinhaAtual[indiceColunaLat], 
							"" + colunasDaLinhaAtual[indiceColunaLng]);

				// requisição no geoservice
				respostaDoServidor = "";
				for (int i = 0; i < arrColunasConfig.length; i++) {
					// verifica se será preciso fazer uma requisão para o
					// geoservice
					if (!indiceColunas.containsKey(arrColunasConfig[i])) {
						respostaDoServidor = getXmlDaUrl(urlDoGeoService);
						break;
					}
				}

				// procura os campos pedidos
				for (int i = 0; i < arrColunasConfig.length; i++) {
					colunaProcurada = arrColunasConfig[i].trim();

					// se existir no arquivo de entrada pegar valor
					// senão buscar no geoservice
					if (indiceColunas.get(colunaProcurada) != null) {
						colunaAtual = colunasDaLinhaAtual[indiceColunas
								.get(colunaProcurada)];
					} else {
						// filtra valor retornado pelo xml do geoservice
						// usar XPath para especificar caminho no xml
						colunaAtual = retornaTag(respostaDoServidor,
								colunaProcurada);
					}

					// delimitador para texto
					if (colunaAtual.startsWith(text_delimeter)) {
						colunaAtual += text_delimeter;
					} else {
						colunaAtual = text_delimeter + colunaAtual
								+ text_delimeter;
					}

					
					linhaSaida.append(colunaAtual + delimeter);
				}

				// para acompanhamento no console
				System.out.println(numLinhasLidas + ". " + urlDoGeoService);

				// escreve registro em arquivo de saida
				escritorArqSaida.println(retiraUltimoDelimiter(linhaSaida
						.toString()));

				// minimiza esforço do servidor
				if ((numLinhasLidas % 50) == 0) {
					try {
						Thread.sleep(5000L);
					} catch (InterruptedException e) {
					}
				}
				
				if ((numLinhasLidas % 500) == 0) {
					try {
						Thread.sleep(10000L);
					} catch (InterruptedException e) {
					}
				}
			}

			leitorArqEntrada.close();
			escritorArqSaida.close();

		} catch (IOException e) {
			System.err.println("Arquivo nao existe.");
			e.printStackTrace();
		}

		// finaliza processo e mostra quanto tempo demorou
		fim = System.currentTimeMillis();
		System.out.println("Fim. " + ((fim - inicio) / 1000L) + " segs");
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

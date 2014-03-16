
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

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
 * Script para convers�o de arquivos. UDH
 * 
 * @author Thiago <tsprates@hotmail.com>
 */
public class ScriptConversor implements Runnable {

	private File inputfile;
	private File outputfile;
	private FileInputStream configFile;
	private Long inicio, fim;
	
	 // propriedades de configura��o
	private Properties props = new Properties();
	
	//delimeter
	private String delimeter;
	private String text_delimeter;
	
	// nome coluna lat e lng para pesquisa na url geoservice
	private String inputfile_col_lat, inputfile_col_lng;
	private String outputfile_cols;
	
	
	private Scanner leitorArqEntrada;
	private PrintStream escritorArqSaida;
	
	// parse xml
	private DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	private DocumentBuilder db;
	private XPathFactory xpathFactory = XPathFactory.newInstance();
	
	public static void main(String[] args) {
		System.out.println(" *SCRIPT para convers�o:");
		
		if (args.length > 0) {
			try {
				new ScriptConversor(args[0]);
			} catch (FileNotFoundException e) {
				System.err.println(" [Error] - Necess�rio Arquivo de Config.");
				mostraAjuda();
			}
		} else {
			System.err.println(" [Error] - Necess�rio Arquivo de Config.");
			mostraAjuda();
		}
	}

	/**
	 * Cria��o de arquivo de configura��o.
	 * 
	 */
	private static void mostraAjuda() {
		System.out.println(" Exemplo de arquivo de configura��o:					");
		System.out.println(" *config.properties										");
		System.out.println("========================================================");
		System.out.println("														");
		System.out.println("inputfile=/caminho/para/arquivo/entrada.csv				");
		System.out.println("outputfile=/caminho/para/arquivo/saida.csv				");
		System.out.println("delimiter=,												");
		System.out.println("text_delimiter=\"										");
		System.out.println("inputfile_col_lat=lat									");
		System.out.println("inputfile_col_lng=lng									");
		System.out.println("geoservice=http://api.geo.org/find?lat=%s&lng=%s		");
		System.out.println("outputfile_cols=A,B,C,D									");
	}

	/**
	 * Define arquivo de entrada para leitura dos dados e arquivo de sa�da para
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
			throw new RuntimeException(e);
		}
		
		inputfile = new File(props.getProperty("inputfile"));
		outputfile = new File(props.getProperty("outputfile"));
		
		//delimeter
		delimeter = props.getProperty("delimiter");
		text_delimeter = props.getProperty("text_delimiter");
		
		// nomes colunas lat e lng da config
		inputfile_col_lat = props.getProperty("inputfile_col_lat");
		inputfile_col_lng = props.getProperty("inputfile_col_lng");
		
		// colunas de sa�da
		outputfile_cols = props.getProperty("outputfile_cols");
		
		// inicio processo
		System.out.println(" Processando:");

		if (inputfile.exists()) {
			new Thread(this).start();
		} else {
			throw new RuntimeException("Arquivos inv�lidos.");
		}
		
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException("[Erro] leitor de xml.");
		}
	}

	

	@Override
	public void run() {
		try {
			leitorArqEntrada = new Scanner(inputfile);
			escritorArqSaida = new PrintStream(outputfile);
			
			// linha que ser� registrado no arquivo de saida para formata��o
			StringBuilder linhaSaida = new StringBuilder();
			
			String linhaAtual;
			
			// colunas (propriedades) requeridas pelo cliente no arquivo de configura��o
			String arrColunasConfig[] = outputfile_cols.split(",");
			
			// mapeamento das colunas
			Map<String, Integer> indiceColunas = new HashMap<String, Integer>();
			
			
			String colunaProcurada, colunaAtual; 
			String respostaDoServidor;
			
			String urlDoGeoService;
			int numLinhasLidas = -1; // desconsidera linha de cabe�alho

			boolean jaExisteNomesDasColunas = false;
			int indiceColunaLat, indiceColunaLng;

			// marca tempo para ver tempo demorado no processo
			inicio = System.currentTimeMillis();

			while (leitorArqEntrada.hasNextLine()) {
				linhaAtual = leitorArqEntrada.nextLine();
				String colunasDaLinhaAtual[] = linhaAtual.split(delimeter);

				// quantidade de linhas lidas do arquivo
				numLinhasLidas++;
				
				// reseta linha de saida para novo registro
				linhaSaida = new StringBuilder("");

				// para descobrir cabe�alho cont�m os nomes das colunas
				if (jaExisteNomesDasColunas == false) {
					// faz mapeamente de colunas do arquivo de entrada 
					// para vincular a uma posi��o da coluna
					for (int i = 0; i < colunasDaLinhaAtual.length; i++) {
						indiceColunas.put(colunasDaLinhaAtual[i], i);
					}
					
					// escreve cabe�alho arquivo de sa�da
					escritorArqSaida.println(props.getProperty("outputfile_cols"));					
					
					// sabe o nome das colunas do arquivo, 
					// previne n�o fazer novamente o mapeamento
					jaExisteNomesDasColunas = true;
					continue;
				}

				// nomes das colunas necess�rios para identificar colunas
				// requiridas no arquivo de configura��es
				if (jaExisteNomesDasColunas == false) {
					throw new RuntimeException("[Erro] N�o foi poss�vel identificar as colunas do arquivo.");
				}
				
				
				if (indiceColunas.get(inputfile_col_lat) == null || indiceColunas.get(inputfile_col_lng) == null) {
					throw new RuntimeException("[Erro] Erro ao especificar colunas de latitude e longitude do arquivo de configura��o, "
							+ "possivelmente problema em definer o 'delimeter'.");
				}
				
				
				// posic�o das colunas: latitude e longitude no arquivo de entrada 
				indiceColunaLat = indiceColunas.get(inputfile_col_lat);
				indiceColunaLng = indiceColunas.get(inputfile_col_lng);
				
				// formata url de requisi��o do geoservice com a latitude e longitude da linha atual
				urlDoGeoService = String.format(
					Locale.ENGLISH,
					props.getProperty("geoservice"),
					"" + colunasDaLinhaAtual[ indiceColunaLat ],
					"" + colunasDaLinhaAtual[ indiceColunaLng ]
				);
				
				
				// para acompanhamento no console
				System.out.println(numLinhasLidas + ") " + urlDoGeoService);


				// requisi��o no geoservice
				respostaDoServidor = "";
				for (int i = 0; i < arrColunasConfig.length; i++) {
					// verifica se ser� preciso fazer uma requis�o para o geoservice 
					if (!indiceColunas.containsKey(arrColunasConfig[i])) {
						respostaDoServidor = getXmlDaUrl(urlDoGeoService);
						break;
					}
				}
				
				// procura os campos pedidos
				for (int i = 0; i < arrColunasConfig.length; i++) {
					colunaProcurada = arrColunasConfig[i];

					// se existir no arquivo de entrada pegar valor
					// sen�o buscar no geoservice
					if (indiceColunas.get(colunaProcurada) != null) {
						colunaAtual = colunasDaLinhaAtual[ indiceColunas.get(colunaProcurada) ];
					} else {
						// filtra valor retornado pelo xml do geoservice
						colunaAtual = retornaTag(respostaDoServidor, colunaProcurada);
					}
					
					// delimitador para texto
					if (colunaAtual.startsWith(text_delimeter)) {
						colunaAtual += text_delimeter;
					} else {
						colunaAtual = text_delimeter + colunaAtual + text_delimeter;
					}
					
					linhaSaida.append( colunaAtual + delimeter );
				}
				
				escritorArqSaida.println(retiraUltimoDelimiter(linhaSaida.toString()));
				
				// minimiza esfor�o do servidor a cada 100 linhas lidas
				if ((numLinhasLidas % 100) == 0) { try { Thread.sleep(350L); } catch (InterruptedException e) {} }
			}

			leitorArqEntrada.close();
			escritorArqSaida.close();

		} catch (FileNotFoundException e) {
			System.err.println("Arquivo n�o existe.");
			e.printStackTrace();
		}

		// finaliza processo e mostra quanto tempo demorou
		fim = System.currentTimeMillis();
		System.out.println("Fim. " + ((fim - inicio) / 1000L) + " segs");
	}
	
	
	/**
	 * A partir de uma string de url para um servi�o ler o conte�do xml.
	 * 
	 * @param url Url do georservice.
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private String getXmlDaUrl(String url) {
		InputStream is;
		try {
			is = new URL(url).openStream();
		} catch (IOException e) {
			return "";
		}
		
		Scanner s = new Scanner(is, "UTF-8");
		StringBuilder sb = new StringBuilder();
		
		while (s.hasNextLine()) {
			sb.append(s.nextLine());
		}
		
		s.close();
		
		return sb.toString();
	}
	
	/**
	 * Filtra no na string xml a tag procurada.
	 * 
	 * @param xml String que representa uma xml de resposta de um webservice
	 * @param tag Tag que procura-se encontrar no xml.
	 * @return Valor da tag encontrado.
	 */
	private String retornaTag(String xml, String tag) {
//		Pattern p = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">",
//				Pattern.UNICODE_CHARACTER_CLASS);
//		Matcher m = p.matcher(xml);
//
//		if (m.find()) {
//			return m.group(1);
//		} else {
//			return "";
//		}
		
		try {
			
			Document document = db.parse( new InputSource(new StringReader(xml)) );
			
			XPath xpath = xpathFactory.newXPath();
			
			return xpath.evaluate(tag, document);
		} catch (SAXException | IOException | XPathExpressionException e) {
			return "";
		} 

		
		
		
	}

	/**
	 * Formata para retirar o �ltimo delimiter utilizado pelo arquivo de csv de entrada.
	 * 
	 * @param s 
	 * @return String
	 */
	private String retiraUltimoDelimiter(String s) {
		return s.substring(0, s.length()
				- props.getProperty("delimiter").length());
	}

}


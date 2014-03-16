
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
 * Script para conversão de arquivos. UDH
 * 
 * @author Thiago <tsprates@hotmail.com>
 */
public class ScriptConversor implements Runnable {

	private File inputfile;
	private File outputfile;
	private FileInputStream configFile;
	private Long inicio, fim;
	
	 // propriedades de configuração
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
		System.out.println(" *SCRIPT para conversão:");
		
		if (args.length > 0) {
			try {
				new ScriptConversor(args[0]);
			} catch (FileNotFoundException e) {
				System.err.println(" [Error] - Necessário Arquivo de Config.");
				mostraAjuda();
			}
		} else {
			System.err.println(" [Error] - Necessário Arquivo de Config.");
			mostraAjuda();
		}
	}

	/**
	 * Criação de arquivo de configuração.
	 * 
	 */
	private static void mostraAjuda() {
		System.out.println(" Exemplo de arquivo de configuração:					");
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
		
		// colunas de saída
		outputfile_cols = props.getProperty("outputfile_cols");
		
		// inicio processo
		System.out.println(" Processando:");

		if (inputfile.exists()) {
			new Thread(this).start();
		} else {
			throw new RuntimeException("Arquivos inválidos.");
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
			
			// linha que será registrado no arquivo de saida para formatação
			StringBuilder linhaSaida = new StringBuilder();
			
			String linhaAtual;
			
			// colunas (propriedades) requeridas pelo cliente no arquivo de configuração
			String arrColunasConfig[] = outputfile_cols.split(",");
			
			// mapeamento das colunas
			Map<String, Integer> indiceColunas = new HashMap<String, Integer>();
			
			
			String colunaProcurada, colunaAtual; 
			String respostaDoServidor;
			
			String urlDoGeoService;
			int numLinhasLidas = -1; // desconsidera linha de cabeçalho

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

				// para descobrir cabeçalho contém os nomes das colunas
				if (jaExisteNomesDasColunas == false) {
					// faz mapeamente de colunas do arquivo de entrada 
					// para vincular a uma posição da coluna
					for (int i = 0; i < colunasDaLinhaAtual.length; i++) {
						indiceColunas.put(colunasDaLinhaAtual[i], i);
					}
					
					// escreve cabeçalho arquivo de saída
					escritorArqSaida.println(props.getProperty("outputfile_cols"));					
					
					// sabe o nome das colunas do arquivo, 
					// previne não fazer novamente o mapeamento
					jaExisteNomesDasColunas = true;
					continue;
				}

				// nomes das colunas necessários para identificar colunas
				// requiridas no arquivo de configurações
				if (jaExisteNomesDasColunas == false) {
					throw new RuntimeException("[Erro] Não foi possível identificar as colunas do arquivo.");
				}
				
				
				if (indiceColunas.get(inputfile_col_lat) == null || indiceColunas.get(inputfile_col_lng) == null) {
					throw new RuntimeException("[Erro] Erro ao especificar colunas de latitude e longitude do arquivo de configuração, "
							+ "possivelmente problema em definer o 'delimeter'.");
				}
				
				
				// posicão das colunas: latitude e longitude no arquivo de entrada 
				indiceColunaLat = indiceColunas.get(inputfile_col_lat);
				indiceColunaLng = indiceColunas.get(inputfile_col_lng);
				
				// formata url de requisição do geoservice com a latitude e longitude da linha atual
				urlDoGeoService = String.format(
					Locale.ENGLISH,
					props.getProperty("geoservice"),
					"" + colunasDaLinhaAtual[ indiceColunaLat ],
					"" + colunasDaLinhaAtual[ indiceColunaLng ]
				);
				
				
				// para acompanhamento no console
				System.out.println(numLinhasLidas + ") " + urlDoGeoService);


				// requisição no geoservice
				respostaDoServidor = "";
				for (int i = 0; i < arrColunasConfig.length; i++) {
					// verifica se será preciso fazer uma requisão para o geoservice 
					if (!indiceColunas.containsKey(arrColunasConfig[i])) {
						respostaDoServidor = getXmlDaUrl(urlDoGeoService);
						break;
					}
				}
				
				// procura os campos pedidos
				for (int i = 0; i < arrColunasConfig.length; i++) {
					colunaProcurada = arrColunasConfig[i];

					// se existir no arquivo de entrada pegar valor
					// senão buscar no geoservice
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
				
				// minimiza esforço do servidor a cada 100 linhas lidas
				if ((numLinhasLidas % 100) == 0) { try { Thread.sleep(350L); } catch (InterruptedException e) {} }
			}

			leitorArqEntrada.close();
			escritorArqSaida.close();

		} catch (FileNotFoundException e) {
			System.err.println("Arquivo não existe.");
			e.printStackTrace();
		}

		// finaliza processo e mostra quanto tempo demorou
		fim = System.currentTimeMillis();
		System.out.println("Fim. " + ((fim - inicio) / 1000L) + " segs");
	}
	
	
	/**
	 * A partir de uma string de url para um serviço ler o conteúdo xml.
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
	 * Formata para retirar o último delimiter utilizado pelo arquivo de csv de entrada.
	 * 
	 * @param s 
	 * @return String
	 */
	private String retiraUltimoDelimiter(String s) {
		return s.substring(0, s.length()
				- props.getProperty("delimiter").length());
	}

}


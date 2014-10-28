script-geoservice
=================

#Arquivo de configuração necessário para rodar o script.


## Uso:

```shell
java -jar script.jar [config.properties] 
```


## Arquivo de Configuração:

- Arquivos de entrada e saída. Obs.: para especificar o caminho do arquivo deve-se utilizar a barra invertida '/', evitar erro. Ex: **C:\users\[USUÁRIO]\desktop\centroide_ud_BH2.csv** deve ficar: **C:/users/[USUÁRIO]/desktop/centroide_ud_BH2.csv**, em Windows como em SO Unix/Linux.
 ``` inputfile=c:/users/username/desktop/qryUDH_BH.csv ``` 
 ``` outputfile=c:/users/username/desktop/s.csv ``` 
 
- Delimitador do arquivo csv para diferenciar as colunas.
 ``` delimiter=, ``` 
 ``` text_delimiter=\ ``` 
 
- Colunas do arquivo de entrada que representam a 'latitude' e 'longitude' (obrigatório).
 ``` inputfile_col_lat=lat ```
 ``` inputfile_col_lng=long ```
 
- Serviço de geocode, deve-se usar %s para inidicar os valores dos parâmetros de latitude e longitude, nessa ordem, necessário pela url do serviço. São definidos pelas colunas: **inputfile_column_lat** e **inputfile_column_lng**.
 ``` geoservice=http://maps.googleapis.com/maps/api/geocode/xml?latlng=%s,%s&sensor=false ```
 
- Lista de colunas (separadas por delimiter). Para pegar a coluna do arquivo de entrada, defina o nome da coluna. Caso queira valor de um geoservice utilizar XPath. Obs.: valores retornados pelo geoservice são navegados através de XPath. Ex: Resposta do servidor: *'&lt;response&gt;&lt;a&gt;something&lt;/a&gt;&lt;b&gt;another thing&lt;/b&gt;&lt;response&gt;'* para recuperar o valor dentro da tag 'a' dentro da tag 'response' use '/response/a'.
  ``` outputfile_cols=UDH_ATLAS,/GeocodeResponse/result/address_component/long_name,/GeocodeResponse/result/geometry/location/lat,/GeocodeResponse/result/geometry/location/lng,lat,long ``` 



### Exemplo de um arquivo de configuração:

 ``` 
 inputfile=c:/users/username/desktop/qryUDH_BH.csv
 outputfile=c:/users/username/desktop/s.csv
 delimiter=,
 text_delimiter=\"
 inputfile_col_lat=lat
 inputfile_col_lng=long
 geoservice=http://maps.googleapis.com/maps/api/geocode/xml?latlng=%s,%s&sensor=false
 outputfile_cols=UDH_ATLAS,/GeocodeResponse/result/address_component/long_name,/GeocodeResponse/result/geometry/location/lat,/GeocodeResponse/result/geometry/location/lng,lat,long  
 ``` 


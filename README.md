script-geoservice
=================

Arquivo de configuração necessário para rodar o script.
-----------------------------------------------------------

# Arquivos de entrada e saída.
# Obs.: para especificar o caminho do arquivo deve-se utilizar a barra invertida '/'. 
# Ex: c:\users\thiago\desktop\centroide_ud_BH2.csv deve ficar: c:/users/thiago/desktop/centroide_ud_BH2.csv
#
inputfile=c:/users/thiago/desktop/qryUDH_BH.csv
outputfile=c:/users/thiago/desktop/s.csv


# Delimitador do arquivo csv para diferenciar as colunas.
#
delimiter=,
text_delimiter=\"


# Colunas do arquivo de entrada que representam a 'latitude' e 'longitude' (obrigatório)
# necessários para a consulta no serviço. Ver [geoservice].
#
inputfile_col_lat=lat
inputfile_col_lng=long


# Serviço utilizado de geocode, deve-se usar %s para inidicar os valores 
# que serão substituidos pelos valores de latitude e longitude, 
# definidos nas colunas: inputfile_column_lat e inputfile_column_lng.
#
geoservice=http://maps.googleapis.com/maps/api/geocode/xml?latlng=%s,%s&sensor=false


# Lista de colunas (separadas pelo valor do delimiter) do geoservice, sempre comece '/' (root).
# Obs.: valores retornados pelo geoservice podem ser navegados por '/', através de XPath.
# Ex: Response do servidor '<response><a>something</a><b>another thing</b><response>'
# para recuperar o valor dentro da tag '<a>' use '/response/a'.
#
outputfile_cols=UDH_ATLAS,/GeocodeResponse/result/address_component/long_name,/GeocodeResponse/result/geometry/location/lat,/GeocodeResponse/result/geometry/location/lng,lat,long

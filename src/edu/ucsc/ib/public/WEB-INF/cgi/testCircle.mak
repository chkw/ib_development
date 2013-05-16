# circlePlot.py plots the image files using  the matplotlib python library

DATA_DIR = Data/paradigm

MATRIX_FILE = $(DATA_DIR)/final_pert.tab

OTHER_DATA_FILES = $(DATA_DIR)/final_para.tab $(DATA_DIR)/final_rppa.tab

OUTPUT_DIR = paradigm_3_ring_no_cosmic_20120301

test:

plot:
	fill.pl -pad $(MATRIX_FILE) \
	> 1.tmp ;
	\
	cut -f 1,2 1.tmp \
	> activities.tmp ;
	\
	cut -f 1,3- 1.tmp \
	> perturbations.tmp ;
	\
	head -n 1 perturbations.tmp \
	| transpose.pl \
	| tail -n +2 \
	> sampleIDs.tmp ;
	\
	cut -f 1 1.tmp \
	| tail -n +2 \
	> features.tmp ;
	\
	mkdir -p $(OUTPUT_DIR) ;
	\
	python2.7 circlePlot.py \
		-s sampleIDs.tmp \
		-f features.tmp \
		-o PSEN2 \
		-m \
		-c activities.tmp \
		$(OUTPUT_DIR) \
		$(OTHER_DATA_FILES) perturbations.tmp ;
	\
	tar -zcvf $(OUTPUT_DIR).tar.gz $(OUTPUT_DIR) ;
	\
	rm -f 1.tmp activities.tmp perturbations.tmp sampleIDs.tmp features.tmp ;
	\

sync:
	rsync -avP ~/ibnightly/ib_cgi/circlePlot.py .

clean:
	rm -rf $(OUTPUT_DIR) $(OUTPUT_DIR).tar.gz ;
	\
	rm -rf output/$(OUTPUT_DIR) output/$(OUTPUT_DIR).tar.gz ;
	\


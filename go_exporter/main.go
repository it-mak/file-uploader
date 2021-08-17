package main

import (
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"strings"
	"time"
)

var (
	filesUploaded = promauto.NewGauge(prometheus.GaugeOpts{
		Name: "files_uploaded",
		Help: "The total number of uploaded files",
	})
)

func countFiles() {
	go func() {
		PathForUploaded := os.Getenv("PATH_FOR_UPLOADED")
		if len(PathForUploaded) == 0 {
			PathForUploaded = "/tmp/shared/"
		}
		for {
			files,_ := ioutil.ReadDir(PathForUploaded)
			var FilteredArray []string
			for _, value := range files{
				if strings.Contains(value.Name(), "uploaded") {
					FilteredArray = append(FilteredArray, value.Name())
				}
			}
			filesUploaded.Set(float64(len(FilteredArray)))
			time.Sleep(2 * time.Second)
		}
	}()
}

func checkAuth(r *http.Request) bool {
	if len(os.Getenv("TOKEN")) == 0 {
		return false
	}
	if r.Header.Get("Authorization") == os.Getenv("TOKEN"){
		return true
	} else {
		return false
	}

}

func main() {
	countFiles()
	r := prometheus.NewRegistry()
	r.MustRegister(filesUploaded)
	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		if checkAuth(r) {
			if r.RequestURI == "/metrics" {
				promhttp.Handler().ServeHTTP(w,r)
			} else {
				_, err := w.Write([]byte("404 Not found\n"))
				if err != nil {
					log.Fatalln("Unable to write response", err)
				}
			}
			return
		}
		_, err := w.Write([]byte("401 Unauthorized\n"))
		if err != nil {
			log.Fatalln("Unable to write response", err)
		}
	})
	err := http.ListenAndServe(":2112", nil)
	if err != nil {
		log.Fatalln("Unable to bind port", err)
	}
}

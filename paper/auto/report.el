(TeX-add-style-hook
 "report"
 (lambda ()
   (TeX-add-to-alist 'LaTeX-provided-class-options
                     '(("article" "11pt")))
   (TeX-add-to-alist 'LaTeX-provided-package-options
                     '(("geometry" "margin=1.5in")))
   (TeX-run-style-hooks
    "latex2e"
    "article"
    "art11"
    "graphicx"
    "geometry")
   (LaTeX-add-labels
    "arch"
    "agg"
    "times"
    "outs")
   (LaTeX-add-bibitems
    "trust"
    "rmi"
    "aes"
    "book"))
 :latex)


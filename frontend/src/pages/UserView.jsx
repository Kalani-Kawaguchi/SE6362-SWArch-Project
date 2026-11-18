import SearchInterface from "../shared/SearchInterface.jsx";
import SearchResult from "../SearchResult.jsx";
import "../styles.css";

export default function Search() {
    return (
        <SearchInterface
            ResultComponent={SearchResult}
        />
    );
}
/*
 * This file is part of Butter.
 *
 * Butter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Butter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Butter. If not, see <http://www.gnu.org/licenses/>.
 */

package butter.droid.tv.ui.detail.movie;

import android.os.Bundle;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v4.app.Fragment;
import butter.droid.base.content.preferences.PreferencesHandler;
import butter.droid.base.torrent.StreamInfo;
import butter.droid.provider.base.module.Media;
import butter.droid.provider.base.module.Movie;
import butter.droid.provider.base.module.Torrent;
import butter.droid.tv.presenters.MovieDetailsDescriptionPresenter;
import butter.droid.tv.ui.detail.TVMediaDetailActivity;
import butter.droid.tv.ui.detail.base.TVBaseDetailsFragment;
import butter.droid.tv.ui.loading.TVStreamLoadingActivity;
import butter.droid.tv.ui.trailer.TVTrailerPlayerActivity;
import javax.inject.Inject;
import org.parceler.Parcels;

public class TVMovieDetailsFragment extends TVBaseDetailsFragment implements TVMovieDetailsView {

    @Inject TVMovieDetailsPresenter presenter;
    @Inject PreferencesHandler preferencesHandler;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((TVMediaDetailActivity) getActivity())
                .getComponent()
                .movieDetailComponentBuilder()
                .movieDetailModule(new TVMovieDetailModule(this))
                .build()
                .inject(this);

        Bundle arguments = getArguments();
        final int providerId = arguments.getInt(EXTRA_PROVIDER);
        final Movie item = Parcels.unwrap(arguments.getParcelable(EXTRA_ITEM));

        presenter.onCreate(providerId, item);
    }

    @Override protected AbstractDetailsDescriptionPresenter getDetailPresenter() {
        return new MovieDetailsDescriptionPresenter();
    }

    @Override public void startTrailer(final Movie movie, final String trailer) {
        startActivity(TVTrailerPlayerActivity.getIntent(getActivity(), movie, trailer));
    }

    @Override public void startMovie(final Movie item, final Torrent torrent, final String quality) {
        String subtitleLanguage = preferencesHandler.getSubtitleDefaultLanguage();
        StreamInfo info = new StreamInfo(torrent, item, null);

        TVStreamLoadingActivity.startActivity(getActivity(), info);
    }

    public static Fragment newInstance(final int providerId, final Media media) {
        TVMovieDetailsFragment fragment = new TVMovieDetailsFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_PROVIDER, providerId);
        bundle.putParcelable(EXTRA_ITEM, Parcels.wrap(media));

        fragment.setArguments(bundle);
        return fragment;
    }

}